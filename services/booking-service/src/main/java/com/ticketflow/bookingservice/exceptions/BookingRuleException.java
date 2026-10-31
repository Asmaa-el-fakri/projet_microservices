package com.ticketflow.bookingservice.exceptions;

public class BookingRuleException extends RuntimeException {
    public BookingRuleException(String message) {
        super(message);
    }
}