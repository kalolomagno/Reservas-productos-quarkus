package com.nttdata.apps.reservation.producer.mapper;

import com.nttdata.apps.reservation.producer.dto.ReservationResponse;
import com.nttdata.apps.reservation.producer.entity.Reservation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCustomerName(),
                reservation.getExternalProductId(),
                reservation.getProductTitle(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
