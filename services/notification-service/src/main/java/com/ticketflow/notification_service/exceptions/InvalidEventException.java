package com.ticketflow.notification_service.exceptions;

/** Message Kafka invalide ou non conforme au contrat : non rejouable, envoyé en DLT. */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
