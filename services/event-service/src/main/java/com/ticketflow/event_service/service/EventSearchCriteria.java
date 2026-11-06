package com.ticketflow.event_service.service;

import java.math.BigDecimal;
import java.time.Instant;

public record EventSearchCriteria(
        String q,
        String city,
        String category,
        Instant dateFrom,
        Instant dateToExclusive,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
}