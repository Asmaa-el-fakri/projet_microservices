package com.ticketflow.ticketservice.dto;

import com.ticketflow.ticketservice.entities.Ticket;
import com.ticketflow.ticketservice.entities.TicketStatus;
import java.time.Instant;

public record TicketResponse(
        Long id,
        Long bookingId,
        Long customerId,
        String verificationCode,
        TicketStatus status,
        Instant issuedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getBookingId(),
                ticket.getCustomerId(),
                ticket.getVerificationCode(),
                ticket.getStatus(),
                ticket.getIssuedAt()
        );
    }
}