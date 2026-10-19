package com.nttdata.apps.reservation.producer.client;

import com.nttdata.apps.reservation.producer.client.dto.DummyJsonProductResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/products")
@RegisterRestClient(configKey = "dummyjson-api")
@Produces(MediaType.APPLICATION_JSON)
public interface DummyJsonProductClient {

    @GET
    @Path("/{productId}")
    DummyJsonProductResponse findProductById(
            @PathParam("productId") Integer productId
    );
}
