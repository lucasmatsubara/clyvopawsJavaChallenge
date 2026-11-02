package br.com.fiap.clyvopaws.auth;

import br.com.fiap.clyvopaws.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final long segundos;

    public TokenService(JwtEncoder encoder, @Value("${auth.access-token-seconds:900}") long segundos) {
        if (segundos < 60 || segundos > 3600) throw new IllegalArgumentException("JWT deve durar entre 60 e 3600 segundos.");
        this.encoder = encoder; this.segundos = segundos;
    }

    public long validadeSegundos() { return segundos; }

    public String generateToken(User user, String sessaoId) {
        Instant agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("clyvopaws-api").issuedAt(agora).expiresAt(agora.plusSeconds(segundos))
                .subject(user.getUsername()).id(UUID.randomUUID().toString())
                .claim("role", user.getRole()).claim("sid", sessaoId).build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}