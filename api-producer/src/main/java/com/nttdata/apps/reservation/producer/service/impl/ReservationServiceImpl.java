package com.nttdata.apps.reservation.producer.service.impl;

import com.nttdata.apps.reservation.producer.dto.CreateReservationRequest;
import com.nttdata.apps.reservation.producer.dto.ReservationResponse;
import com.nttdata.apps.reservation.producer.dto.UpdateReservationRequest;
import com.nttdata.apps.reservation.producer.entity.Reservation;
import com.nttdata.apps.reservation.producer.mapper.ReservationMapper;
import com.nttdata.apps.reservation.producer.repository.ReservationRepository;
import com.nttdata.apps.reservation.producer.service.ExternalProduct;
import com.nttdata.apps.reservation.producer.service.ExternalProductService;
import com.nttdata.apps.reservation.producer.service.ReservationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final ExternalProductService externalProductService;

    public ReservationServiceImpl(
            ReservationRepository reservationRepository,
            ReservationMapper reservationMapper,
            ExternalProductService externalProductService
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationMapper = reservationMapper;
        this.externalProductService = externalProductService;
    }

    @Override
    @Transactional
    public ReservationResponse create(CreateReservationRequest request) {
        ExternalProduct externalProduct =
                externalProductService.getAvailableProduct(
                        request.externalProductId(),
                        request.quantity()
                );

        Reservation reservation = new Reservation(
                request.customerName().trim(),
                externalProduct.id(),
                externalProduct.title(),
                request.quantity()
        );

        reservationRepository.persist(reservation);

        return reservationMapper.toResponse(reservation);
    }

    @Override
    public List<ReservationResponse> findAll() {
        return reservationRepository.listLatestFirst()
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    public ReservationResponse findById(UUID reservationId) {
        return reservationMapper.toResponse(
                findReservationById(reservationId)
        );
    }

    @Override
    @Transactional
    public ReservationResponse update(
            UUID reservationId,
            UpdateReservationRequest request
    ) {
        Reservation reservation = findReservationById(reservationId);

        ExternalProduct externalProduct =
                externalProductService.getAvailableProduct(
                        reservation.getExternalProductId(),
                        request.quantity()
                );

        reservation.updateDetails(
                request.customerName().trim(),
                request.quantity(),
                externalProduct.title()
        );

        return reservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse cancel(UUID reservationId) {
        Reservation reservation = findReservationById(reservationId);

        reservation.cancel();

        return reservationMapper.toResponse(reservation);
    }

    private Reservation findReservationById(UUID reservationId) {
        return reservationRepository.findByIdOptional(reservationId)
                .orElseThrow(() -> new NotFoundException(
                        "Reservation with ID %s was not found."
                                .formatted(reservationId)
                ));
    }
}
