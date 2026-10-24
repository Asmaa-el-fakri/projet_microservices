package com.ticketflow.bookingservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookingRequest(
        @NotNull Long customerId,
        @NotNull Long eventId,
        @Positive @Max(10) int quantity) {
}