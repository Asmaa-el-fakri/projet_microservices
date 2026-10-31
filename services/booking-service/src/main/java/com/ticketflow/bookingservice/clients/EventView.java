package com.ticketflow.bookingservice.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventView(Long id, String title, Instant eventDate,
                        BigDecimal price, int capacity, String status) {
}