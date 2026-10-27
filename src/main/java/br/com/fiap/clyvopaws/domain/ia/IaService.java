package br.com.fiap.clyvopaws.domain.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;

@Service
public class IaService {

    private static final long MAX_FOTO = 5L * 1024 * 1024;

    private static final Set<String> TIPOS =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final RestClient client;
    private final String apiKey;

    public IaService(
            @Value("${ia.base-url}") String baseUrl,
            @Value("${ia.api-key:}") String apiKey) {

        this.apiKey = apiKey.trim();

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(60_000);

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public AnalisePetResponseDTO analisar(MultipartFile foto) {

        if (apiKey.length() < 32) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure IA_SERVICE_API_KEY no servidor Java."
            );
        }

        if (foto == null || foto.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecione uma foto."
            );
        }

        if (foto.getSize() > MAX_FOTO) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "A foto deve ter no máximo 5 MiB."
            );
        }

        String tipo = foto.getContentType();

        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Envie uma foto JPEG, PNG ou WebP."
            );
        }

        try {
            var arquivo = new ByteArrayResource(foto.getBytes()) {
                @Override
                public String getFilename() {
                    return "pet";
                }
            };

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(tipo));

            var body = new LinkedMultiValueMap<String, Object>();
            body.add("foto", new HttpEntity<>(arquivo, headers));

            var resultado = client.post()
                    .uri("/api/ia/analisar-pet")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            (request, response) -> {
                                throw erroDoServico(
                                        response.getStatusCode().value()
                                );
                            }
                    )
                    .body(AnalisePetResponseDTO.class);

            if (resultado == null
                    || resultado.status() == null
                    || !Boolean.TRUE.equals(
                    resultado.requerConfirmacaoTutor())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "O serviço de IA retornou uma resposta inválida."
                );
            }

            return resultado;

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não foi possível ler a foto."
            );

        } catch (ResourceAccessException e) {
            if (e.getMostSpecificCause() instanceof SocketTimeoutException) {
                throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "A análise demorou para responder. Tente novamente."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível conectar à IA. Confira se o Python está rodando."
            );

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Não foi possível interpretar a resposta da IA."
            );
        }
    }

    private ResponseStatusException erroDoServico(int codigo) {
        return switch (codigo) {
            case 400, 422 -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A foto não pôde ser analisada. Envie outra imagem válida."
            );

            case 413 -> new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "A foto excede o limite de tamanho ou resolução."
            );

            case 415 -> new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formato de imagem não suportado pelo serviço de IA."
            );

            case 401, 403 -> new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A chave interna do Java não corresponde à do serviço de IA."
            );

            case 429, 503 -> new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "IA indisponível. Confira a configuração e a cota do provedor."
            );

            case 504 -> new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "A IA demorou para responder. Tente novamente."
            );

            default -> new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falha na resposta do serviço de IA."
            );
        };
    }
}