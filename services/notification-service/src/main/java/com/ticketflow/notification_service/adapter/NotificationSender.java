package com.ticketflow.notification_service.adapter;

import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationChannel;
import com.ticketflow.notification_service.exceptions.DeliveryException;

/** Port de sortie d'un canal de notification (email aujourd'hui, SMS/Telegram plus tard). */
public interface NotificationSender {

    NotificationChannel channel();

    /**
     * Envoie la notification.
     *
     * @return le nom du fournisseur ayant traité l'envoi (ex. "smtp", "log")
     * @throws DeliveryException si l'envoi échoue
     */
    String send(Notification notification) throws DeliveryException;
}
