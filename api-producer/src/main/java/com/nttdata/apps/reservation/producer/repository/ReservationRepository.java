package com.nttdata.apps.reservation.producer.repository;

import com.nttdata.apps.reservation.producer.entity.Reservation;
import com.nttdata.apps.reservation.producer.entity.ReservationStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReservationRepository implements PanacheRepositoryBase<Reservation, UUID> {

    public List<Reservation> findByExternalProductId(Integer externalProductId) {
        return list("externalProductId", externalProductId);
    }

    public List<Reservation> findByStatus(ReservationStatus status) {
        return list("status", status);
    }
}
