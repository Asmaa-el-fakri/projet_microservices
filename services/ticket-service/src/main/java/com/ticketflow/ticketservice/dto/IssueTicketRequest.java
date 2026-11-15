package com.ticketflow.ticketservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IssueTicketRequest(
        @NotNull @Positive Long bookingId,
        @NotNull @Positive Long customerId,
        @Positive int quantity
) {
}