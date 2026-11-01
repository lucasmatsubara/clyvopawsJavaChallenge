package br.com.fiap.clyvopaws.auth;

public record SessionResponse(
        String token, String nomeCompleto, String fotoUrl, Long id, String role,
        String refreshToken, long expiresIn, long refreshExpiresAt
) {}