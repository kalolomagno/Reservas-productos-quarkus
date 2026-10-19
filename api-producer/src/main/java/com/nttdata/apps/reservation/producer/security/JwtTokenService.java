package com.nttdata.apps.reservation.producer.security;

import com.nttdata.apps.reservation.producer.dto.TokenResponse;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class JwtTokenService {

    private final SecurityProperties securityProperties;

    public JwtTokenService(
            SecurityProperties securityProperties
    ) {
        this.securityProperties = securityProperties;
    }

    public TokenResponse generateToken(
            AuthenticatedClient authenticatedClient
    ) {
        Integer expirationMinutes =
                securityProperties.jwt().expirationMinutes();

        String token = Jwt.issuer(
                        securityProperties.jwt().issuer()
                )
                .subject(authenticatedClient.clientId())
                .groups(Set.of(authenticatedClient.role()))
                .expiresIn(Duration.ofMinutes(expirationMinutes))
                .sign();

        return new TokenResponse(
                token,
                "Bearer",
                expirationMinutes.longValue() * 60,
                authenticatedClient.role()
        );
    }
}
