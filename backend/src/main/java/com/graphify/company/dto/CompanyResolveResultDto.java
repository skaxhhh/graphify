package com.graphify.company.dto;

public record CompanyResolveResultDto(
        Long id,
        String name,
        String ticker,
        String syncStatus,
        boolean created
) {}
