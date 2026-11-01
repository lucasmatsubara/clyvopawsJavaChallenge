package br.com.fiap.clyvopaws.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RefreshRecusadoException extends ResponseStatusException {
    public RefreshRecusadoException() {
        super(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada. Faça login novamente.");
    }
}