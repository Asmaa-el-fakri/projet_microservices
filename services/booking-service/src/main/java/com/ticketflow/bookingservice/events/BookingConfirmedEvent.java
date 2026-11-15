
package com.ticketflow.bookingservice.events;

import java.time.Instant;

public record BookingConfirmedEvent(
        Long bookingId,
        Long customerId,
        Long eventId,
        int quantity,
        Instant occurredAt
) {
}
