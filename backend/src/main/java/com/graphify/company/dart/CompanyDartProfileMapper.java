package com.graphify.company.dart;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.company.dto.CompanyDartProfileDto;
import com.graphify.company.dto.CompanyNewsItemDto;
import com.graphify.company.dto.DisclosureSummaryDto;
import com.graphify.company.dto.FinancialStatementLineDto;
import com.graphify.company.registry.dart.DartDisclosureItem;
import com.graphify.company.registry.dart.DartFinancialLine;
import com.graphify.home.news.ExternalNewsArticle;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CompanyDartProfileMapper {

    private final ObjectMapper objectMapper;

    public CompanyDartProfileMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CompanyDartProfileDto toProfileDto(CompanyDartSnapshot snapshot) {
        try {
            JsonNode profile = objectMapper.readTree(snapshot.getProfileJson());
            List<DartDisclosureItem> disclosures = readList(snapshot.getDisclosuresJson(), DartDisclosureItem.class);
            List<DartFinancialLine> financials = readList(snapshot.getFinancialsJson(), DartFinancialLine.class);
            List<ExternalNewsArticle> news = readList(snapshot.getNewsJson(), ExternalNewsArticle.class);

            List<DisclosureSummaryDto> recent = disclosures.stream()
                    .map(d -> new DisclosureSummaryDto(
                            d.receiptNo(),
                            d.receiptDate(),
                            d.reportName(),
                            d.submitter()
                    ))
                    .toList();
            List<FinancialStatementLineDto> financialDtos = financials.stream()
                    .map(f -> new FinancialStatementLineDto(
                            f.bsnsYear(),
                            f.reprtCode(),
                            f.reportLabel(),
                            f.accountName(),
                            f.currentAmount(),
                            f.previousAmount(),
                            f.currency()
                    ))
                    .toList();
            List<CompanyNewsItemDto> newsDtos = news.stream()
                    .map(n -> new CompanyNewsItemDto(
                            n.title(),
                            n.summary(),
                            n.sourceName(),
                            n.sourceUrl(),
                            n.publishedAt()
                    ))
                    .toList();

            return new CompanyDartProfileDto(
                    text(profile, "corp_name"),
                    text(profile, "stock_code"),
                    text(profile, "ceo_nm"),
                    mapCorpClass(text(profile, "corp_cls")),
                    text(profile, "adres"),
                    text(profile, "hm_url"),
                    text(profile, "induty_code"),
                    text(profile, "est_dt"),
                    text(profile, "acc_mt"),
                    text(profile, "bizr_no"),
                    recent,
                    financialDtos,
                    newsDtos,
                    snapshot.getCollectedAt().toString()
            );
        } catch (Exception ex) {
            return null;
        }
    }

    public String buildAgentContext(CompanyDartProfileDto profile, String companyName, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 기업\n");
        sb.append("- 이름: ").append(companyName).append('\n');
        if (profile.corpName() != null && !profile.corpName().equals(companyName)) {
            sb.append("- DART 정식명: ").append(profile.corpName()).append('\n');
        }
        if (profile.stockCode() != null) {
            sb.append("- 종목코드: ").append(profile.stockCode()).append('\n');
        }
        if (profile.corpClassLabel() != null) {
            sb.append("- 시장: ").append(profile.corpClassLabel()).append('\n');
        }
        if (profile.ceoName() != null) {
            sb.append("- 대표: ").append(profile.ceoName()).append('\n');
        }
        if (profile.industryCode() != null) {
            sb.append("- 업종코드: ").append(profile.industryCode()).append('\n');
        }
        if (profile.estDate() != null) {
            sb.append("- 설립일: ").append(profile.estDate()).append('\n');
        }
        if (profile.accMonth() != null) {
            sb.append("- 결산월: ").append(profile.accMonth()).append('\n');
        }
        if (profile.bizrNo() != null) {
            sb.append("- 사업자번호: ").append(profile.bizrNo()).append('\n');
        }
        if (profile.address() != null) {
            sb.append("- 주소: ").append(profile.address()).append('\n');
        }
        if (profile.homepage() != null) {
            sb.append("- 홈페이지: ").append(profile.homepage()).append('\n');
        }
        if (summary != null && !summary.isBlank()) {
            sb.append("- 요약: ").append(summary.trim()).append('\n');
        }

        if (!profile.financialStatements().isEmpty()) {
            sb.append("\n## 재무제표 (DART 주요계정)\n");
            int limit = Math.min(profile.financialStatements().size(), 16);
            for (int i = 0; i < limit; i++) {
                FinancialStatementLineDto f = profile.financialStatements().get(i);
                sb.append("- [").append(f.bsnsYear()).append(" ").append(f.reportLabel()).append("] ");
                sb.append(f.accountName()).append(": ").append(f.currentAmount() != null ? f.currentAmount() : "—");
                if (f.previousAmount() != null && !f.previousAmount().isBlank()) {
                    sb.append(" (전기 ").append(f.previousAmount()).append(')');
                }
                sb.append('\n');
            }
        }

        if (!profile.recentDisclosures().isEmpty()) {
            sb.append("\n## 최근 공시 (최대 6개월)\n");
            int limit = Math.min(profile.recentDisclosures().size(), 15);
            for (int i = 0; i < limit; i++) {
                DisclosureSummaryDto d = profile.recentDisclosures().get(i);
                sb.append("- [").append(d.receiptDate() != null ? d.receiptDate() : "—").append("] ");
                sb.append(d.reportName());
                if (d.submitter() != null && !d.submitter().isBlank()) {
                    sb.append(" (").append(d.submitter()).append(')');
                }
                sb.append('\n');
            }
        }

        if (!profile.relatedNews().isEmpty()) {
            sb.append("\n## 관련 뉴스\n");
            int limit = Math.min(profile.relatedNews().size(), 12);
            for (int i = 0; i < limit; i++) {
                CompanyNewsItemDto n = profile.relatedNews().get(i);
                sb.append("- ");
                if (n.publishedAt() != null) {
                    sb.append('[').append(n.publishedAt()).append("] ");
                }
                sb.append(n.title());
                if (n.sourceName() != null) {
                    sb.append(" — ").append(n.sourceName());
                }
                if (n.summary() != null && !n.summary().isBlank()) {
                    sb.append(": ").append(truncate(n.summary(), 120));
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private <T> List<T> readList(String json, Class<T> elementType) throws Exception {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
        );
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replace('\n', ' ');
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "...";
    }

    private static String mapCorpClass(String corpCls) {
        if (corpCls == null || corpCls.isBlank()) {
            return null;
        }
        return switch (corpCls.trim().toUpperCase(Locale.ROOT)) {
            case "Y" -> "KOSPI";
            case "K" -> "KOSDAQ";
            case "N" -> "KONEX";
            case "E" -> "기타";
            default -> corpCls;
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String raw = value.asText().trim();
        return raw.isEmpty() ? null : raw;
    }
}
