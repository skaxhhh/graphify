package com.graphify.company.registry;

import java.util.Optional;

public record ExternalCompanyCandidate(
        String externalSource,
        String externalId,
        String name,
        String ticker,
        String market,
        boolean listed
) {
    public Optional<String> tickerOptional() {
        if (ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ticker.trim());
    }
}
