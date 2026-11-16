package com.ticketflow.notification_service.exceptions;

/** Échec d'envoi par un canal : la notification est replanifiée selon la politique de reprise. */
public class DeliveryException extends RuntimeException {

    public DeliveryException(String message) {
        super(message);
    }

    public DeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
