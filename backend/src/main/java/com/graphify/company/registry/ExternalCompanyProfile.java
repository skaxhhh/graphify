package com.graphify.company.registry;

public record ExternalCompanyProfile(
        String externalSource,
        String externalId,
        String name,
        String ticker,
        String industry,
        String market,
        boolean listed,
        String summary
) {}
