package com.graphify.admin.dto;

import java.time.Instant;

public record AdminAlertDto(String severity, String message, Instant detectedAt) {
}
