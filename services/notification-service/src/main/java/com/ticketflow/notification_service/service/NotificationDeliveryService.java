package com.ticketflow.notification_service.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.ticketflow.notification_service.adapter.SenderRegistry;
import com.ticketflow.notification_service.config.NotificationProperties;
import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationStatus;
import com.ticketflow.notification_service.repos.NotificationRepository;

/**
 * Livre une notification et met à jour son statut. N'échoue jamais vers l'appelant : un échec d'envoi
 * n'affecte ni le consommateur Kafka ni les opérations métier d'origine ; il est suivi par les tentatives.
 */
@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private final NotificationRepository repository;
    private final SenderRegistry senders;
    private final NotificationProperties properties;
    private final Clock clock;

    public NotificationDeliveryService(NotificationRepository repository, SenderRegistry senders,
                                       NotificationProperties properties, Clock clock) {
        this.repository = repository;
        this.senders = senders;
        this.properties = properties;
        this.clock = clock;
    }

    public void deliver(UUID id) {
        try {
            Notification notification = repository.findById(id).orElse(null);
            if (notification == null || notification.getStatus() != NotificationStatus.PENDING) {
                return;
            }
            attempt(notification);
        } catch (ObjectOptimisticLockingFailureException concurrent) {
            log.debug("Notification {} déjà traitée par une autre instance", id);
        } catch (RuntimeException e) {
            log.error("Erreur inattendue lors de la livraison de la notification {} : {}", id,
                    e.getClass().getSimpleName());
        }
    }

    private void attempt(Notification notification) {
        Instant now = clock.instant();
        try {
            String provider = senders.forChannel(notification.getChannel()).send(notification);
            notification.markSent(now, provider);
        } catch (RuntimeException e) {
            notification.markAttemptFailed(e.getClass().getSimpleName() + ": " + safeMessage(e), now,
                    properties.delivery().maxAttempts(), properties.delivery().retryBaseDelay());
            log.warn("Échec d'envoi : notificationId={}, tentative={}, statut={}", notification.getId(),
                    notification.getAttemptCount(), notification.getStatus());
        }
        repository.save(notification);
    }

    private static String safeMessage(RuntimeException e) {
        String m = e.getMessage();
        return m == null ? "erreur" : m;
    }
}
