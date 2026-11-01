package br.com.fiap.clyvopaws.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class LimpezaSessoes {
    private final RefreshTokenService refresh;

    @Scheduled(initialDelay = 60000, fixedDelay = 86400000)
    public void limpar() { refresh.limparExpiradas(); }
}