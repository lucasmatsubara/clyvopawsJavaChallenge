package br.com.fiap.clyvopaws.auth;

import br.com.fiap.clyvopaws.domain.user.User;
import br.com.fiap.clyvopaws.domain.user.UserRepository;
import br.com.fiap.clyvopaws.domain.tutor.TutorRepository;
import br.com.fiap.clyvopaws.domain.veterinario.VeterinarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final TutorRepository tutores;
    private final VeterinarioRepository veterinarios;
    private final TokenService tokens;
    private final long duracaoMillis;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(JdbcTemplate jdbc, UserRepository users, TutorRepository tutores,
                               VeterinarioRepository veterinarios, TokenService tokens,
                               @Value("${auth.refresh-token-days:7}") long dias) {
        if (dias < 1 || dias > 30) throw new IllegalArgumentException("Refresh deve durar entre 1 e 30 dias.");
        this.jdbc = jdbc; this.users = users; this.tutores = tutores;
        this.veterinarios = veterinarios; this.tokens = tokens;
        this.duracaoMillis = dias * 86400000L;
    }

    private record Sessao(String id, long userId, String atual, String credencial, long expira, boolean revogada) {}

    static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private String novoSegredo() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Sessao buscarSessao(String id, boolean bloquear) {
        return jdbc.query("SELECT id, user_id, refresh_atual, credencial_hash, expira_em, revogada " +
                                "FROM tb_auth_session WHERE id = ?" + (bloquear ? " FOR UPDATE" : ""),
                        (rs, n) -> new Sessao(rs.getString(1), rs.getLong(2), rs.getString(3),
                                rs.getString(4), rs.getLong(5), rs.getInt(6) != 0), id)
                .stream().findFirst().orElse(null);
    }

    private String sessaoDoHash(String tokenHash) {
        return jdbc.query("SELECT sessao_id FROM tb_refresh_token WHERE token_hash = ?",
                (rs, n) -> rs.getString(1), tokenHash).stream().findFirst().orElse(null);
    }

    @Transactional
    public SessionResponse iniciar(String username) {
        User user = users.findByUsername(username).orElseThrow(RefreshRecusadoException::new);
        String id = UUID.randomUUID().toString(), segredo = novoSegredo(), resumo = hash(segredo);
        long expira = System.currentTimeMillis() + duracaoMillis;
        jdbc.update("INSERT INTO tb_auth_session (id, user_id, refresh_atual, credencial_hash, expira_em, revogada) VALUES (?, ?, ?, ?, ?, 0)",
                id, user.getId(), resumo, hash(user.getPassword()), expira);
        jdbc.update("INSERT INTO tb_refresh_token (token_hash, sessao_id) VALUES (?, ?)", resumo, id);
        return resposta(user, id, segredo, expira);
    }

    @Transactional(noRollbackFor = RefreshRecusadoException.class)
    public SessionResponse renovar(String segredo) {
        validarFormato(segredo);
        String resumo = hash(segredo), id = sessaoDoHash(resumo);
        if (id == null) throw new RefreshRecusadoException();
        Sessao sessao = buscarSessao(id, true);
        if (sessao == null || sessao.revogada()) throw new RefreshRecusadoException();
        User user = users.findById(sessao.userId()).orElse(null);
        if (sessao.expira() <= System.currentTimeMillis() || !sessao.atual().equals(resumo) ||
                user == null || !sessao.credencial().equals(hash(user.getPassword()))) {
            revogar(id);
            throw new RefreshRecusadoException();
        }
        String novo = novoSegredo(), novoHash = hash(novo);
        jdbc.update("INSERT INTO tb_refresh_token (token_hash, sessao_id) VALUES (?, ?)", novoHash, id);
        jdbc.update("UPDATE tb_auth_session SET refresh_atual = ? WHERE id = ?", novoHash, id);
        return resposta(user, id, novo, sessao.expira());
    }

    @Transactional
    public void encerrar(String segredo) {
        validarFormato(segredo);
        String id = sessaoDoHash(hash(segredo));
        if (id != null && buscarSessao(id, true) != null) revogar(id);
    }

    private void revogar(String id) {
        jdbc.update("UPDATE tb_auth_session SET revogada = 1 WHERE id = ?", id);
    }

    private void validarFormato(String segredo) {
        if (segredo == null || !segredo.matches("[A-Za-z0-9_-]{43}")) throw new RefreshRecusadoException();
    }

    @Transactional(readOnly = true)
    public boolean aceitaJwt(Jwt jwt) {
        String id = jwt.getClaimAsString("sid");
        if (id == null || !id.matches("[a-f0-9-]{36}")) return false;
        Sessao sessao = buscarSessao(id, false);
        if (sessao == null || sessao.revogada() || sessao.expira() <= System.currentTimeMillis()) return false;
        User user = users.findById(sessao.userId()).orElse(null);
        return user != null && user.getUsername().equals(jwt.getSubject()) &&
                user.getRole().equals(jwt.getClaimAsString("role")) &&
                sessao.credencial().equals(hash(user.getPassword()));
    }

    @Transactional
    public void limparExpiradas() {
        jdbc.update("DELETE FROM tb_auth_session WHERE expira_em <= ?", System.currentTimeMillis());
    }

    private SessionResponse resposta(User user, String sessaoId, String refresh, long expira) {
        String nome = user.getUsername(), foto = null; Long id = null;
        if ("TUTOR".equals(user.getRole())) {
            var tutor = tutores.findByUserUsername(user.getUsername()).orElseThrow(RefreshRecusadoException::new);
            nome = tutor.getNomeCompleto(); foto = tutor.getFotoUrl(); id = tutor.getId();
        } else if ("VETERINARIO".equals(user.getRole())) {
            var vet = veterinarios.findByUserUsername(user.getUsername()).orElseThrow(RefreshRecusadoException::new);
            nome = vet.getNomeCompleto(); foto = vet.getFotoUrl(); id = vet.getId();
        }
        return new SessionResponse(tokens.generateToken(user, sessaoId), nome, foto, id,
                user.getRole(), refresh, tokens.validadeSegundos(), expira);
    }
}