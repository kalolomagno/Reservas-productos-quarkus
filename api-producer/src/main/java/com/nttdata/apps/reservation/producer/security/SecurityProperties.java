package com.nttdata.apps.reservation.producer.security;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app.security")
public interface SecurityProperties {

    Jwt jwt();

    Clients clients();

    interface Jwt {
        String issuer();

        Integer expirationMinutes();
    }

    interface Clients {
        Client operator();

        Client admin();
    }

    interface Client {
        String clientId();

        String clientSecret();

        String role();
    }
}
