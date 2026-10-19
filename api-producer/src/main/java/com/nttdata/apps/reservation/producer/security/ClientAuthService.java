package com.nttdata.apps.reservation.producer.security;

import com.nttdata.apps.reservation.producer.exception.InvalidClientCredentialsException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientAuthService {

    private final SecurityProperties securityProperties;

    public ClientAuthService(
            SecurityProperties securityProperties
    ) {
        this.securityProperties = securityProperties;
    }

    public AuthenticatedClient authenticate(
            String clientId,
            String clientSecret
    ) {
        SecurityProperties.Client operator =
                securityProperties.clients().operator();

        if (operator.clientId().equals(clientId)
                && operator.clientSecret().equals(clientSecret)) {

            return new AuthenticatedClient(
                    operator.clientId(),
                    operator.role()
            );
        }

        SecurityProperties.Client admin =
                securityProperties.clients().admin();

        if (admin.clientId().equals(clientId)
                && admin.clientSecret().equals(clientSecret)) {

            return new AuthenticatedClient(
                    admin.clientId(),
                    admin.role()
            );
        }

        throw new InvalidClientCredentialsException();
    }
}
