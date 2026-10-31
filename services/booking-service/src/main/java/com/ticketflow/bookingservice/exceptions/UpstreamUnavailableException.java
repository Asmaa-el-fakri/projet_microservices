package com.ticketflow.bookingservice.exceptions;

public class UpstreamUnavailableException extends RuntimeException {
    public UpstreamUnavailableException(String service, Throwable cause) {
        super("The " + service + " is temporarily unavailable, please retry", cause);
    }
}