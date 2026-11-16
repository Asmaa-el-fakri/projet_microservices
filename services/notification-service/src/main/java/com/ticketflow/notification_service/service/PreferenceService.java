package com.ticketflow.notification_service.service;

import com.ticketflow.notification_service.entities.NotificationChannel;

/**
 * Point d'extension pour les préférences et désabonnements. Elles appartiennent au Customer Service :
 * une implémentation future les obtiendra par API interne contrôlée ou par projection d'événements
 * (aucune copie du profil client ici). Les notifications transactionnelles ne passent pas par ce contrôle.
 */
public interface PreferenceService {

    boolean isAllowed(String recipient, SupportedEventType type, NotificationChannel channel);
}
