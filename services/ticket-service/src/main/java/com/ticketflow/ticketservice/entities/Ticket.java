package com.ticketflow.ticketservice.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "tickets",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "verification_code")
        }
)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, unique = true, length = 64)
    private String verificationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.VALID;

    @Column(nullable = false)
    private Instant issuedAt = Instant.now();

    protected Ticket() {
    }

    public Ticket(Long bookingId, Long customerId, String verificationCode) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.verificationCode = verificationCode;
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void markAsUsed() {
        this.status = TicketStatus.USED;
    }

    public void cancel() {
        this.status = TicketStatus.CANCELLED;
    }
}