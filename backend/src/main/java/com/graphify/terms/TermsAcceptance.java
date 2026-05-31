package com.graphify.terms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "terms_acceptances")
public class TermsAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "terms_document_id", nullable = false)
    private Long termsDocumentId;

    @Column(name = "terms_version", nullable = false)
    private String termsVersion;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @PrePersist
    void onCreate() {
        acceptedAt = Instant.now();
    }

    public static TermsAcceptance of(Long userId, Long termsDocumentId, String termsVersion) {
        TermsAcceptance entity = new TermsAcceptance();
        entity.userId = userId;
        entity.termsDocumentId = termsDocumentId;
        entity.termsVersion = termsVersion;
        return entity;
    }
}
