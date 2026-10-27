package br.com.fiap.clyvopaws.auth;

import br.com.fiap.clyvopaws.domain.tutor.TutorRepository;
import br.com.fiap.clyvopaws.domain.user.User;
import br.com.fiap.clyvopaws.domain.user.UserRepository;
import br.com.fiap.clyvopaws.domain.veterinario.VeterinarioRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Login", description = "Endpoints para login de usuários")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token, String nomeCompleto, String fotoUrl, Long id, String role) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request){
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        String username = auth.getName();
        String token = tokenService.generateToken(username);
        String role = userRepository.findByUsername(username).map(User::getRole).orElse(null);

        // Perfis sem registro próprio (ADMIN) ficam com o username como nome e id/foto nulos.
        String nome = username;
        String foto = null;
        Long id = null;

        if ("TUTOR".equals(role)) {
            var tutor = tutorRepository.findByUserUsername(username).orElse(null);
            if (tutor != null) {
                nome = tutor.getNomeCompleto();
                foto = tutor.getFotoUrl();
                id = tutor.getId();
            }
        } else if ("VETERINARIO".equals(role)) {
            var vet = veterinarioRepository.findByUserUsername(username).orElse(null);
            if (vet != null) {
                nome = vet.getNomeCompleto();
                foto = vet.getFotoUrl();
                id = vet.getId();
            }
        }

        return new LoginResponse(token, nome, foto, id, role);
    }

}