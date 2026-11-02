package br.com.fiap.clyvopaws.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Login", description = "Login, renovação e encerramento de sessão")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refresh;

    public record LoginRequest(@NotBlank @Size(max = 100) String username,
                               @NotBlank @Size(max = 256) String password) {}
    public record RefreshRequest(@NotBlank @Size(max = 128) String refreshToken) {}

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        var autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return resposta(refresh.iniciar(autenticacao.getName()));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<SessionResponse> renovar(@Valid @RequestBody RefreshRequest request) {
        return resposta(refresh.renovar(request.refreshToken()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> sair(@Valid @RequestBody RefreshRequest request) {
        refresh.encerrar(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private ResponseEntity<SessionResponse> resposta(SessionResponse dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache").body(dados);
    }
}