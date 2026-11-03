package br.com.fiap.clyvopaws.domain.foto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LeitorDadosCadastro {
    private final ObjectMapper mapper;
    private final Validator validator;

    public <T> T ler(String json, Class<T> tipo) {
        if (json == null || json.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do cadastro.");
        }
        if (json.length() > 12_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do cadastro muito extensos.");
        }
        try {
            T dados = mapper.readValue(json, tipo);
            if (dados == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do cadastro.");
            var erros = validator.validate(dados);
            if (!erros.isEmpty()) {
                String mensagem = erros.stream().map(e -> e.getPropertyPath() + ": " + e.getMessage())
                        .sorted().collect(Collectors.joining("; "));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
            }
            return dados;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo dados deve conter um JSON válido.");
        }
    }
}