package com.ticketflow.notification_service.service;

import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.entities.NotificationChannel;

/**
 * Implémentation par défaut tant qu'aucune source de préférences n'est branchée : tout est autorisé.
 * Pour brancher les vraies préférences, remplacer cette classe par une implémentation de PreferenceService.
 */
@Component
public class AllowAllPreferenceService implements PreferenceService {

    @Override
    public boolean isAllowed(String recipient, SupportedEventType type, NotificationChannel channel) {
        return true;
    }
}
