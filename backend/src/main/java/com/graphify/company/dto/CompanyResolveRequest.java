package com.graphify.company.dto;

public record CompanyResolveRequest(
        String query,
        String ticker,
        String externalSource,
        String externalId
) {}
