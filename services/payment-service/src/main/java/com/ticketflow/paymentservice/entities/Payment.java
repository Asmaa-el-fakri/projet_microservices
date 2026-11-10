package com.ticketflow.paymentservice.entities;

import com.ticketflow.paymentservice.exceptions.PaymentRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    // "sub" du jeton Keycloak du client qui paie
    @Column(nullable = false, length = 100)
    private String ownerId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = createdAt;

    protected Payment() {
        // requis par JPA
    }

    public Payment(Long bookingId, String ownerId, BigDecimal amount, String currency) {
        this.bookingId = bookingId;
        this.ownerId = ownerId;
        this.amount = amount;
        this.currency = currency;
    }

    public void succeed() {
        transitionTo(PaymentStatus.SUCCEEDED);
    }

    public void fail() {
        transitionTo(PaymentStatus.FAILED);
    }

    public void cancel() {
        transitionTo(PaymentStatus.CANCELLED);
    }

    // Seul un paiement en attente peut changer d'etat : les etats finaux sont definitifs
    private void transitionTo(PaymentStatus target) {
        if (status != PaymentStatus.PENDING) {
            throw new PaymentRuleException(
                    "Paiement " + id + " : transition " + status + " vers " + target + " interdite");
        }
        status = target;
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public String getOwnerId() { return ownerId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}