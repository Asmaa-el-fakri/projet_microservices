
package com.ticketflow.paymentservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.paymentservice.events.PaymentSucceededEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(PaymentSucceededEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    "payment.succeeded",
                    event.bookingId().toString(),
                    message
            );

            log.info("Payment succeeded event published for booking {}",
                    event.bookingId());

        } catch (JsonProcessingException e) {
            log.error("Could not serialize payment event", e);
        }
    }
}
