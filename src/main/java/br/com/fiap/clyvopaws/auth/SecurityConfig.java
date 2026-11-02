package br.com.fiap.clyvopaws.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @ConfigurationProperties(prefix = "rsa")
    public record RsaKeyProperties(RSAPublicKey publicKey, RSAPrivateKey privateKey) {}

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Login: o AuthController está mapeado em "/login" (sem prefixo "/auth").
                        // Precisa ser público, senão ninguém consegue gerar token.
                        .requestMatchers(HttpMethod.POST, "/login", "/auth/refresh", "/auth/logout").permitAll()
                        // Somente leitura de imagens por UUID aleatório, sem endpoint de listagem.
                        // Não libera os dados de tutores/pets nem suas operações de escrita.
                        .requestMatchers(HttpMethod.GET, "/fotos/*").permitAll()
                        // Autocadastro de tutor: precisa ser público, pois é o próprio tutor
                        // criando sua conta (cria o User em cascata dentro de TutorService).
                        .requestMatchers(HttpMethod.POST, "/tutores").permitAll()
                        // Cadastro de veterinário só pode ser feito por um perfil administrativo.
                        .requestMatchers(HttpMethod.POST, "/veterinarios").hasRole("ADMIN")
                        // Quem exatamente é o "próprio dono"/"próprio veterinário" é checado
                        // dentro dos services (AuthorizationService), não dá pra expressar
                        // isso só com o path aqui.
                        .requestMatchers(HttpMethod.POST, "/agendamentos").hasAnyRole("TUTOR", "ADMIN")
                        // Horários livres de um veterinário: tanto o próprio vet quanto
                        // qualquer tutor (pra escolher horário e agendar) podem ver —
                        // ou seja, basta estar autenticado, não precisa de role específica.
                        // Path real é GET /agendas/veterinario/{id} (AgendaDisponivelController).
                        .requestMatchers(HttpMethod.GET, "/agendas/veterinario/**").authenticated()

                        // Clínicas e catálogo preventivo: dado de referência, leitura livre
                        // pra qualquer autenticado, escrita só ADMIN.
                        .requestMatchers(HttpMethod.POST, "/clinicas", "/planos-preventivos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/clinicas/**", "/planos-preventivos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/clinicas/**", "/planos-preventivos/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                ))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtDecoder jwtDecoder(RsaKeyProperties rsaKeyProperties, RefreshTokenService refresh){
        var decoder = NimbusJwtDecoder.withPublicKey(rsaKeyProperties.publicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("clyvopaws-api"),
                jwt -> refresh.aceitaJwt(jwt) ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sessão encerrada.", null))
        ));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(RsaKeyProperties rsaKeyProperties){
        var privateKey = rsaKeyProperties.privateKey();
        var publicKey = rsaKeyProperties.publicKey();
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        ImmutableJWKSet<SecurityContext> jwtSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwtSource);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("role");

        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}