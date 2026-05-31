package com.graphify.company.dart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "company_dart_snapshots")
public class CompanyDartSnapshot {

    @Id
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "corp_code", nullable = false, length = 8)
    private String corpCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_json", nullable = false, columnDefinition = "jsonb")
    private String profileJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "disclosures_json", nullable = false, columnDefinition = "jsonb")
    private String disclosuresJson = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "financials_json", nullable = false, columnDefinition = "jsonb")
    private String financialsJson = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "news_json", nullable = false, columnDefinition = "jsonb")
    private String newsJson = "[]";

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt = Instant.now();

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCorpCode() {
        return corpCode;
    }

    public void setCorpCode(String corpCode) {
        this.corpCode = corpCode;
    }

    public String getProfileJson() {
        return profileJson;
    }

    public void setProfileJson(String profileJson) {
        this.profileJson = profileJson;
    }

    public String getDisclosuresJson() {
        return disclosuresJson;
    }

    public void setDisclosuresJson(String disclosuresJson) {
        this.disclosuresJson = disclosuresJson;
    }

    public String getFinancialsJson() {
        return financialsJson;
    }

    public void setFinancialsJson(String financialsJson) {
        this.financialsJson = financialsJson;
    }

    public String getNewsJson() {
        return newsJson;
    }

    public void setNewsJson(String newsJson) {
        this.newsJson = newsJson;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }
}
