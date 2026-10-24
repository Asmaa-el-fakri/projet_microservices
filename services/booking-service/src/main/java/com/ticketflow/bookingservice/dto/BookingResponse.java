package com.ticketflow.bookingservice.dto;

import com.ticketflow.bookingservice.entities.Booking;
import com.ticketflow.bookingservice.entities.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(
        Long id,
        Long customerId,
        Long eventId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        BookingStatus status,
        Instant createdAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getCustomerId(), b.getEventId(),
                b.getQuantity(), b.getUnitPrice(), b.getTotalPrice(),
                b.getStatus(), b.getCreatedAt());
    }
}