package com.ticketflow.paymentservice.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "ticketflow.events.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaPaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;
    private final String succeededTopic;
    private final String failedTopic;

    public KafkaPaymentEventPublisher(
            KafkaTemplate<String, String> kafka,
            JsonMapper json,
            @Value("${ticketflow.kafka.topics.payment-succeeded:ticketflow.payment.succeeded}") String succeededTopic,
            @Value("${ticketflow.kafka.topics.payment-failed:ticketflow.payment.failed}") String failedTopic) {
        this.kafka = kafka;
        this.json = json;
        this.succeededTopic = succeededTopic;
        this.failedTopic = failedTopic;
    }

    // Envoi seulement apres le commit de la base. Un echec de Kafka n'annule jamais le paiement.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentEvent(PaymentEvent event) {
        String key = String.valueOf(event.data().bookingId());
        String topic = PaymentEvent.TYPE_SUCCEEDED.equals(event.eventType()) ? succeededTopic : failedTopic;
        try {
            String payload = json.writeValueAsString(event);
            kafka.send(topic, key, payload).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Echec de publication de {} pour la reservation {}", event.eventType(), key, error);
                } else {
                    log.info("Evenement {} publie sur {} pour la reservation {}", event.eventType(), topic, key);
                }
            });
        } catch (RuntimeException e) {
            log.error("Echec de publication de {} pour la reservation {}", event.eventType(), key, e);
        }
    }
}