package com.nttdata.apps.reservation.producer.service;

import com.nttdata.apps.reservation.producer.dto.CreateReservationRequest;
import com.nttdata.apps.reservation.producer.dto.ReservationResponse;
import com.nttdata.apps.reservation.producer.dto.UpdateReservationRequest;

import java.util.List;
import java.util.UUID;

public interface ReservationService {

    ReservationResponse create(CreateReservationRequest request);

    List<ReservationResponse> findAll();

    ReservationResponse findById(UUID reservationId);

    ReservationResponse update(
            UUID reservationId,
            UpdateReservationRequest request
    );

    ReservationResponse cancel(UUID reservationId);
}
