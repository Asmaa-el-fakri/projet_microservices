package com.ticketflow.notification_service.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationChannel;

/**
 * Implémentation de développement : aucun email n'est envoyé. La notification est seulement
 * journalisée (sans contenu ni adresse complète) et enregistrée avec provider=log.
 */
@Component
@ConditionalOnProperty(name = "notification.email.mode", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements NotificationSender {

    public static final String PROVIDER = "log";

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String send(Notification n) {
        log.info("[DEV] Email NON envoyé (mode log) : notificationId={}, type={}, destinataire={}",
                n.getId(), n.getEventType(), maskEmail(n.getRecipient()));
        return PROVIDER;
    }

    static String maskEmail(String email) {
        if (email == null) {
            return "?";
        }
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}
