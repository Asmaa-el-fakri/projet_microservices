package com.ticketflow.customerservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone
) {}