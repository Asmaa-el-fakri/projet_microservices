package com.ticketflow.bookingservice.clients;

import java.math.BigDecimal;
import java.time.Instant;

public record EventView(
        Long id,
        String title,
        String summary,
        Instant eventDate,
        String city,
        String venue,
        String category,
        BigDecimal price,
        int capacity,
        String status) {
}