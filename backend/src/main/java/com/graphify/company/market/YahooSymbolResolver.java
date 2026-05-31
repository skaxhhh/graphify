package com.graphify.company.market;

import java.util.Locale;
import java.util.Optional;

public final class YahooSymbolResolver {

    private YahooSymbolResolver() {
    }

    public static Optional<String> resolve(String ticker, String market) {
        if (ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        String code = ticker.trim();
        if (market == null || market.isBlank()) {
            return Optional.of(code + ".KS");
        }
        String normalized = market.trim().toUpperCase(Locale.ROOT);
        String suffix = switch (normalized) {
            case "KOSDAQ", "KDQ" -> ".KQ";
            case "KOSPI", "KRX", "KSC" -> ".KS";
            case "KONEX" -> ".KN";
            default -> ".KS";
        };
        return Optional.of(code + suffix);
    }
}
