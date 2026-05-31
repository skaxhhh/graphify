package com.graphify.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_alerts")
public class AdminAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String message;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    public String getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }
}
