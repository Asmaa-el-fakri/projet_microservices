package com.ticketflow.event_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

public record EventRequest(
        @NotBlank String title,
        String summary,
        @NotNull @Future Instant eventDate,
        @NotBlank String city,
        @NotBlank String venue,
        @NotBlank String category,
        @NotNull @PositiveOrZero BigDecimal price,
        @Positive int capacity) {
}