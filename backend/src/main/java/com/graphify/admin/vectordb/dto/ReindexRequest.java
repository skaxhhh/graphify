package com.graphify.admin.vectordb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record ReindexRequest(
        @NotBlank
        @Pattern(regexp = "ALL|SELECTED")
        String scope,
        List<Long> targetIds
) {
}
