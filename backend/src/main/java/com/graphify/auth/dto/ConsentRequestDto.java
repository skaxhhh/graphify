package com.graphify.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ConsentRequestDto(
        @NotEmpty List<Long> acceptedTermIds,
        @NotBlank String version
) {}
