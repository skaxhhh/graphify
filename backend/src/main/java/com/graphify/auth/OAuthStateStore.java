package com.graphify.auth;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OAuthStateStore {

    private static final long TTL_SECONDS = 600;

    private final Map<String, Entry> states = new ConcurrentHashMap<>();

    public String issue(String provider) {
        cleanup();
        String state = UUID.randomUUID().toString();
        states.put(state, new Entry(provider, Instant.now().plusSeconds(TTL_SECONDS)));
        return state;
    }

    public Optional<String> consume(String state) {
        cleanup();
        Entry entry = states.remove(state);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.provider());
    }

    private void cleanup() {
        Instant now = Instant.now();
        states.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    private record Entry(String provider, Instant expiresAt) {}
}
