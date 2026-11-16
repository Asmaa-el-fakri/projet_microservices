package com.ticketflow.notification_service.exceptions;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("Notification introuvable : " + id);
    }
}
