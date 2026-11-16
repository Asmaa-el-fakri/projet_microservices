package com.ticketflow.notification_service.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.ticketflow.notification_service.config.NotificationProperties;
import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationChannel;
import com.ticketflow.notification_service.repos.NotificationRepository;

/**
 * Transforme un événement validé en enregistrements Notification (idempotent).
 * Volontairement non transactionnel : chaque save est atomique, et une violation de la contrainte
 * d'unicité (doublon concurrent) est interceptée sans invalider le reste du traitement.
 */
@Service
public class NotificationIngestService {

    private static final Logger log = LoggerFactory.getLogger(NotificationIngestService.class);
    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;

    private final EventValidator validator;
    private final NotificationTemplateService templates;
    private final RecipientResolver recipients;
    private final PreferenceService preferences;
    private final NotificationRepository repository;
    private final NotificationProperties properties;
    private final Clock clock;

    public NotificationIngestService(EventValidator validator, NotificationTemplateService templates,
                                     RecipientResolver recipients, PreferenceService preferences,
                                     NotificationRepository repository, NotificationProperties properties,
                                     Clock clock) {
        this.validator = validator;
        this.templates = templates;
        this.recipients = recipients;
        this.preferences = preferences;
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /** @return les identifiants des notifications PENDING créées, à livrer. */
    public List<UUID> handle(EventEnvelope envelope) {
        validator.validate(envelope);

        SupportedEventType type = SupportedEventType.from(envelope.eventType()).orElse(null);
        if (type == null) {
            if (SupportedEventType.isKnownIgnored(envelope.eventType())) {
                log.debug("Événement {} sans notification, ignoré (eventId={})", envelope.eventType(), envelope.eventId());
            } else {
                log.warn("Type d'événement inconnu ignoré : {} (eventId={})", envelope.eventType(), envelope.eventId());
            }
            return List.of();
        }

        List<String> to = recipients.resolve(type, envelope);
        if (to.isEmpty()) {
            log.info("Événement {} sans destinataire, rien à envoyer (eventId={})", type.wireName(), envelope.eventId());
            return List.of();
        }

        NotificationTemplateService.Content content = templates.render(type, envelope);
        Instant now = clock.instant();
        Instant firstAttempt = now.plus(properties.delivery().retryBaseDelay());

        List<UUID> created = new ArrayList<>();
        for (String recipient : to) {
            if (repository.existsByEventIdAndChannelAndRecipient(envelope.eventId(), CHANNEL, recipient)) {
                log.info("Doublon ignoré : eventId={}, type={}", envelope.eventId(), type.wireName());
                continue;
            }
            boolean allowed = type.transactional() || preferences.isAllowed(recipient, type, CHANNEL);
            Notification notification = allowed
                    ? Notification.pending(envelope.eventId(), type.wireName(), envelope.aggregateId(),
                            envelope.correlationId(), recipient, CHANNEL, content.subject(), content.message(),
                            now, firstAttempt)
                    : Notification.skipped(envelope.eventId(), type.wireName(), envelope.aggregateId(),
                            envelope.correlationId(), recipient, CHANNEL, content.subject(), content.message(),
                            now, "Refusé par les préférences du destinataire");
            try {
                repository.saveAndFlush(notification);
            } catch (DataIntegrityViolationException duplicate) {
                log.info("Doublon concurrent ignoré : eventId={}, type={}", envelope.eventId(), type.wireName());
                continue;
            }
            if (allowed) {
                created.add(notification.getId());
            }
        }
        return created;
    }
}
