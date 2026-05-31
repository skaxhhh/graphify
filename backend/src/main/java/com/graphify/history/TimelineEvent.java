package com.graphify.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "timeline_events")
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String label;

    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Instant getEventAt() {
        return eventAt;
    }

    public String getEventType() {
        return eventType;
    }

    public String getLabel() {
        return label;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
