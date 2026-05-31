package com.graphify.terms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "terms_documents")
public class TermsDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public String getContent() {
        return content;
    }

    public boolean isRequired() {
        return required;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
