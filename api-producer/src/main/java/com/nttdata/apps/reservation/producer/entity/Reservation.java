package com.nttdata.apps.reservation.producer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "external_product_id", nullable = false)
    private Integer externalProductId;

    @Column(name = "product_title", nullable = false, length = 255)
    private String productTitle;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() {
    }

    public Reservation(
            String customerName,
            Integer externalProductId,
            String productTitle,
            Integer quantity
    ) {
        this.customerName = customerName;
        this.externalProductId = externalProductId;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.status = ReservationStatus.CREATED;
    }

    @PrePersist
    void beforePersist() {
        Instant now = Instant.now();

        if (status == null) {
            status = ReservationStatus.CREATED;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public void updateDetails(
            String customerName,
            Integer quantity,
            String productTitle
    ) {
        this.customerName = customerName;
        this.quantity = quantity;
        this.productTitle = productTitle;
    }


    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Integer getExternalProductId() {
        return externalProductId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
