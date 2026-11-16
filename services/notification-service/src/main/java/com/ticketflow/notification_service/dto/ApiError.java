package com.ticketflow.notification_service.dto;

import java.time.Instant;

public record ApiError(int status, String code, String message, Instant timestamp) {
}
