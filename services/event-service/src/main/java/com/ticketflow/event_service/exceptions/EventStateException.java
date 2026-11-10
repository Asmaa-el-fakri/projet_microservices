package com.ticketflow.event_service.exceptions;

public class EventStateException extends RuntimeException {
    public EventStateException(String message) {
        super(message);
    }
}