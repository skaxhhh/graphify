package com.graphify.toss;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "toss_credentials")
public class TossCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "client_id_encrypted", nullable = false, columnDefinition = "TEXT")
    private String clientIdEncrypted;

    @Column(name = "client_secret_encrypted", nullable = false, columnDefinition = "TEXT")
    private String clientSecretEncrypted;

    @Column(name = "access_token_encrypted", columnDefinition = "TEXT")
    private String accessTokenEncrypted;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TossCredential() {}

    public TossCredential(Long userId, String clientIdEncrypted, String clientSecretEncrypted) {
        this.userId = userId;
        this.clientIdEncrypted = clientIdEncrypted;
        this.clientSecretEncrypted = clientSecretEncrypted;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId()                      { return id; }
    public Long getUserId()                  { return userId; }
    public String getClientIdEncrypted()     { return clientIdEncrypted; }
    public String getClientSecretEncrypted() { return clientSecretEncrypted; }
    public String getAccessTokenEncrypted()  { return accessTokenEncrypted; }
    public Instant getTokenExpiresAt()       { return tokenExpiresAt; }
    public Instant getCreatedAt()            { return createdAt; }
    public Instant getUpdatedAt()            { return updatedAt; }

    public void setClientIdEncrypted(String clientIdEncrypted) {
        this.clientIdEncrypted = clientIdEncrypted;
    }

    public void setClientSecretEncrypted(String clientSecretEncrypted) {
        this.clientSecretEncrypted = clientSecretEncrypted;
    }

    public void setAccessTokenEncrypted(String accessTokenEncrypted) {
        this.accessTokenEncrypted = accessTokenEncrypted;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }
}
