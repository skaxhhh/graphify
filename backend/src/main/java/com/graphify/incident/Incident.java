package com.graphify.incident;

import java.time.LocalDateTime;

public class Incident {
    private String id;
    private String title;
    private String description;
    private Status status;
    private Severity severity;
    private String service;
    private LocalDateTime occurredAt;
    private LocalDateTime resolvedAt;

    public enum Status { OPEN, INVESTIGATING, RESOLVED, CLOSED }
    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }

    public Incident() {}

    public Incident(String id, String title, String description, Status status, Severity severity,
                    String service, LocalDateTime occurredAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.severity = severity;
        this.service = service;
        this.occurredAt = occurredAt;
        this.resolvedAt = resolvedAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Severity getSeverity() { return severity; }
    public String getService() { return service; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }

    public Long getMttrMinutes() {
        if (occurredAt == null || resolvedAt == null) return null;
        return java.time.Duration.between(occurredAt, resolvedAt).toMinutes();
    }
}
