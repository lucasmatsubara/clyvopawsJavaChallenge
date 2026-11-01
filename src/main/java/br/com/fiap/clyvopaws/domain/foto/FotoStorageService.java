package br.com.fiap.clyvopaws.domain.foto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.Types;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/** Grava somente no Oracle: os binários ficam separados dos cadastros e das listagens. */
@Service
public class FotoStorageService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final String baseUrl;
    private final Semaphore conversoes = new Semaphore(2);

    public FotoStorageService(JdbcTemplate jdbc,
                              @Value("${PUBLIC_API_BASE_URL:https://clyvopawsjavachallenge.onrender.com}") String baseUrl) {
        this.jdbc = jdbc;
        String valor = baseUrl.trim().replaceAll("/+$", "");
        URI uri = URI.create(valor);
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null || valor.length() > 200) {
            throw new IllegalArgumentException("PUBLIC_API_BASE_URL deve ser o endereço público HTTP/HTTPS da API Java.");
        }
        this.baseUrl = valor;
    }

    public String salvarTutor(MultipartFile foto, Long id) { return salvar(foto, id, true); }
    public String salvarPet(MultipartFile foto, Long id) { return salvar(foto, id, false); }

    private String salvar(MultipartFile foto, Long donoId, boolean tutor) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A foto e o cadastro devem usar a mesma transação.");
        }
        if (donoId == null || donoId <= 0) throw new IllegalArgumentException("Cadastro inválido.");
        if (!conversoes.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Há outras fotos sendo processadas. Tente novamente em instantes.");
        }
        byte[] jpeg;
        try { jpeg = prepararJpeg(foto); }
        finally { conversoes.release(); }

        // Esses nomes vêm somente de constantes do servidor, nunca de parâmetros do cliente.
        String tabela = tutor ? "tb_tutor" : "tb_pet";
        String coluna = tutor ? "tutor_id" : "pet_id";
        try {
            // Serializa substituições concorrentes da foto do mesmo cadastro.
            jdbc.queryForObject("SELECT id FROM " + tabela + " WHERE id = ? FOR UPDATE", Long.class, donoId);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado.");
        }
        String id = UUID.randomUUID().toString();
        jdbc.update("DELETE FROM tb_foto WHERE " + coluna + " = ?", donoId);
        jdbc.update("INSERT INTO tb_foto (id, conteudo, tutor_id, pet_id) VALUES (?, ?, ?, ?)", ps -> {
            ps.setString(1, id);
            ps.setBlob(2, new ByteArrayInputStream(jpeg), jpeg.length);
            if (tutor) {
                ps.setLong(3, donoId); ps.setNull(4, Types.NUMERIC);
            } else {
                ps.setNull(3, Types.NUMERIC); ps.setLong(4, donoId);
            }
        });
        return baseUrl + "/fotos/" + id;
    }

    @Transactional(readOnly = true)
    public byte[] buscar(String id) {
        if (id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada.");
        }
        try {
            return jdbc.queryForObject("SELECT conteudo FROM tb_foto WHERE id = ?",
                    (rs, linha) -> rs.getBytes("conteudo"), id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada.");
        }
    }

    /** Confere o conteúdo real, limita pixels e reencoda sem os metadados da foto. */
    static byte[] prepararJpeg(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma foto.");
        }
        if (foto.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter até 5 MiB.");
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(foto.getBytes()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Imagem inválida");
            var reader = readers.next();
            try {
                String formato = reader.getFormatName();
                if (!formato.equalsIgnoreCase("jpeg") && !formato.equalsIgnoreCase("png")) {
                    throw new IOException("Formato inválido");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 12_000_000) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                            "Reduza a foto para até 12 megapixels.");
                }
                BufferedImage original = reader.read(0);
                double escala = Math.min(1.0, 1024.0 / Math.max(width, height));
                var reduzida = new BufferedImage(Math.max(1, (int) (width * escala)),
                        Math.max(1, (int) (height * escala)), BufferedImage.TYPE_INT_RGB);
                var graphics = reduzida.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, reduzida.getWidth(), reduzida.getHeight());
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(original, 0, 0, reduzida.getWidth(), reduzida.getHeight(), null);
                } finally {
                    graphics.dispose();
                    original.flush();
                }
                try (var output = new ByteArrayOutputStream()) {
                    if (!ImageIO.write(reduzida, "jpeg", output)) throw new IOException("JPEG indisponível");
                    return output.toByteArray();
                } finally {
                    reduzida.flush();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Não foi possível ler a foto. Envie uma imagem JPEG ou PNG válida.");
        }
    }

}