
package com.ticketflow.bookingservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.bookingservice.events.PaymentSucceededEvent;
import com.ticketflow.bookingservice.service.BookingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(
            BookingService bookingService,
            ObjectMapper objectMapper) {
        this.bookingService = bookingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "payment.succeeded",
            groupId = "booking-service-group"
    )
    public void consume(String message) {

        try {
            PaymentSucceededEvent event =
                    objectMapper.readValue(
                            message,
                            PaymentSucceededEvent.class
                    );

            log.info(
                    "Received successful payment for booking {}",
                    event.bookingId()
            );

            bookingService.confirmBooking(event.bookingId());

        } catch (Exception e) {
            log.error("Failed to process payment event: {}", message, e);
            throw new IllegalStateException(
                    "Could not process payment event", e
            );
        }
    }
}
