package com.nttdata.apps.reservation.producer.resource;

import com.nttdata.apps.reservation.producer.dto.TokenRequest;
import com.nttdata.apps.reservation.producer.dto.TokenResponse;
import com.nttdata.apps.reservation.producer.security.AuthenticatedClient;
import com.nttdata.apps.reservation.producer.security.ClientAuthService;
import com.nttdata.apps.reservation.producer.security.JwtTokenService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final ClientAuthService clientAuthService;
    private final JwtTokenService jwtTokenService;

    public AuthResource(
            ClientAuthService clientAuthService,
            JwtTokenService jwtTokenService
    ) {
        this.clientAuthService = clientAuthService;
        this.jwtTokenService = jwtTokenService;
    }

    @POST
    @Path("/token")
    @PermitAll
    public Response generateToken(
            @Valid TokenRequest request
    ) {
        AuthenticatedClient authenticatedClient =
                clientAuthService.authenticate(
                        request.clientId(),
                        request.clientSecret()
                );

        TokenResponse response =
                jwtTokenService.generateToken(authenticatedClient);

        return Response.ok(response).build();
    }
}
