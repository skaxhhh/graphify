package com.graphify.admin.vectordb.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CleanupRequest(
        @Min(1) @Max(3650) int olderThanDays,
        @NotEmpty List<@jakarta.validation.constraints.Pattern(regexp = "COMPANY|INSIGHT|RELATION") String> types
) {
}
