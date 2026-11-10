package com.ticketflow.paymentservice.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "ticketflow.events.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaPaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public KafkaPaymentEventPublisher(KafkaTemplate<String, Object> kafka,
                                      @Value("${ticketflow.events.payments-topic:payments.events}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    // Envoi seulement apres le commit de la base. Un echec de Kafka n'annule jamais le paiement.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentEvent(PaymentEvent event) {
        String key = String.valueOf(event.bookingId());
        try {
            kafka.send(topic, key, event).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Echec de publication de {} pour la reservation {}", event.type(), key, error);
                } else {
                    log.info("Evenement {} publie pour la reservation {}", event.type(), key);
                }
            });
        } catch (RuntimeException e) {
            log.error("Echec de publication de {} pour la reservation {}", event.type(), key, e);
        }
    }
}