
package com.ticketflow.paymentservice.events;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSucceededEvent(
        Long paymentId,
        Long bookingId,
        String ownerId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
}
