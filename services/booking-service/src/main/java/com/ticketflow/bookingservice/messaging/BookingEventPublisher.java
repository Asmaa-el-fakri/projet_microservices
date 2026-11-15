
package com.ticketflow.bookingservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.bookingservice.events.BookingConfirmedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(BookingEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BookingEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(BookingConfirmedEvent event) {

        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    "booking.confirmed",
                    event.bookingId().toString(),
                    message
            );

            log.info(
                    "Booking confirmed event published for booking {}",
                    event.bookingId()
            );

        } catch (JsonProcessingException e) {
            log.error("Could not serialize booking event", e);
        }
    }
}
