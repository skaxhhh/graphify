package com.graphify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "graphify.secrets")
public class GraphifySecretsProperties {

    private String encryptionKey = "dev-only-change-me-graphify-secrets-key-256bits-min!!";

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}
