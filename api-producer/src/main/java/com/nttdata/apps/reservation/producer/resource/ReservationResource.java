package com.nttdata.apps.reservation.producer.resource;

import com.nttdata.apps.reservation.producer.dto.CreateReservationRequest;
import com.nttdata.apps.reservation.producer.dto.ReservationResponse;
import com.nttdata.apps.reservation.producer.dto.UpdateReservationRequest;
import com.nttdata.apps.reservation.producer.service.ReservationService;
import io.smallrye.common.annotation.Blocking;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/reservations")
@Blocking
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    private final ReservationService reservationService;

    public ReservationResource(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @POST
    public Response createReservation(
            @Valid CreateReservationRequest request
    ) {
        ReservationResponse response =
                reservationService.create(request);

        URI location = URI.create(
                "/api/reservations/" + response.id()
        );

        return Response.created(location)
                .entity(response)
                .build();
    }

    @GET
    public List<ReservationResponse> getReservations() {
        return reservationService.findAll();
    }

    @GET
    @Path("/{reservationId}")
    public ReservationResponse getReservationById(
            @PathParam("reservationId") UUID reservationId
    ) {
        return reservationService.findById(reservationId);
    }

    @PUT
    @Path("/{reservationId}")
    public ReservationResponse updateReservation(
            @PathParam("reservationId") UUID reservationId,
            @Valid UpdateReservationRequest request
    ) {
        return reservationService.update(reservationId, request);
    }

    @DELETE
    @Path("/{reservationId}")
    public ReservationResponse cancelReservation(
            @PathParam("reservationId") UUID reservationId
    ) {
        return reservationService.cancel(reservationId);
    }
}
