package com.ticketflow.paymentservice.dto;

import jakarta.validation.constraints.NotNull;

public record SimulationRequest(@NotNull Outcome outcome) {

    public enum Outcome {
        SUCCESS, FAILURE, CANCEL
    }
}