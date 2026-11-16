package com.ticketflow.notification_service.messaging;

import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.service.NotificationDeliveryService;
import com.ticketflow.notification_service.service.NotificationIngestService;

/**
 * Consommateur des événements de domaine. Les erreurs d'ingestion remontent au DefaultErrorHandler
 * (reprises puis DLT) ; la livraison, elle, ne lève jamais d'exception.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final EventParser parser;
    private final NotificationIngestService ingest;
    private final NotificationDeliveryService delivery;

    public NotificationEventListener(EventParser parser, NotificationIngestService ingest,
                                     NotificationDeliveryService delivery) {
        this.parser = parser;
        this.ingest = ingest;
        this.delivery = delivery;
    }

    @KafkaListener(topics = "${notification.kafka.topics}", groupId = "${notification.kafka.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        EventEnvelope envelope = parser.parse(record.value());
        log.info("Événement reçu : type={}, eventId={}, correlationId={}, topic={}", envelope.eventType(),
                envelope.eventId(), envelope.correlationId(), record.topic());
        List<UUID> created = ingest.handle(envelope);
        created.forEach(delivery::deliver);
    }
}
