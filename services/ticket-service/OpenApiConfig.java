package com.ticketflow.ticketservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TicketFlow - Ticket Service API",
                version = "1.0",
                description = "API for issuing, retrieving and verifying tickets."
        )
)
public class OpenApiConfig {
}