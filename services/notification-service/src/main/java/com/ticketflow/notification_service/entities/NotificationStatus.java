package com.ticketflow.notification_service.entities;

public enum NotificationStatus {
    /** Créée, en attente d'envoi (ou d'une nouvelle tentative). */
    PENDING,
    /** Envoyée (ou journalisée en mode développement : voir le champ provider). */
    SENT,
    /** Nombre maximal de tentatives atteint. */
    FAILED,
    /** Non envoyée car le destinataire a refusé ce type de notification. */
    SKIPPED
}
