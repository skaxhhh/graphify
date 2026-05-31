package com.graphify.history.dto;

import java.time.Instant;

public record DiffSummaryDto(String text, Instant generatedAt) {
}
