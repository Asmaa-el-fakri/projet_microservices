package com.ticketflow.paymentservice.exceptions;

// Regle metier violee : transition interdite, paiement deja actif...
public class PaymentRuleException extends RuntimeException {

    public PaymentRuleException(String message) {
        super(message);
    }
}