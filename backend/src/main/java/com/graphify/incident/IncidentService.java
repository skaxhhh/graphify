package com.graphify.incident;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private static final List<Incident> MOCK_DATA = List.of(
        new Incident("INC-001", "결제 서비스 응답 지연", "결제 API 평균 응답시간 10초 초과", Incident.Status.CLOSED, Incident.Severity.CRITICAL, "payment", LocalDateTime.of(2026, 5, 1, 9, 0), LocalDateTime.of(2026, 5, 1, 10, 30)),
        new Incident("INC-002", "인증 서버 간헐적 오류", "JWT 검증 실패 5xx 오류 발생", Incident.Status.CLOSED, Incident.Severity.HIGH, "auth", LocalDateTime.of(2026, 5, 3, 14, 0), LocalDateTime.of(2026, 5, 3, 15, 0)),
        new Incident("INC-003", "DB 커넥션 풀 고갈", "PostgreSQL 커넥션 최대치 도달", Incident.Status.RESOLVED, Incident.Severity.CRITICAL, "database", LocalDateTime.of(2026, 5, 10, 3, 0), LocalDateTime.of(2026, 5, 10, 4, 20)),
        new Incident("INC-004", "알림 서비스 지연", "푸시 알림 발송 최대 30분 지연", Incident.Status.CLOSED, Incident.Severity.MEDIUM, "notification", LocalDateTime.of(2026, 5, 15, 11, 0), LocalDateTime.of(2026, 5, 15, 12, 0)),
        new Incident("INC-005", "검색 API 오류율 증가", "Elasticsearch 쿼리 타임아웃", Incident.Status.RESOLVED, Incident.Severity.HIGH, "search", LocalDateTime.of(2026, 5, 20, 16, 0), LocalDateTime.of(2026, 5, 20, 17, 10)),
        new Incident("INC-006", "결제 서비스 재발 장애", "결제 API 응답 지연 재발 (INC-001 유사)", Incident.Status.CLOSED, Incident.Severity.HIGH, "payment", LocalDateTime.of(2026, 5, 25, 9, 30), LocalDateTime.of(2026, 5, 25, 10, 15)),
        new Incident("INC-007", "배치 작업 실패", "일일 정산 배치 타임아웃", Incident.Status.CLOSED, Incident.Severity.MEDIUM, "batch", LocalDateTime.of(2026, 6, 1, 2, 0), LocalDateTime.of(2026, 6, 1, 3, 30)),
        new Incident("INC-008", "API Gateway 503 오류", "트래픽 급증으로 인한 Gateway 오버로드", Incident.Status.INVESTIGATING, Incident.Severity.CRITICAL, "gateway", LocalDateTime.of(2026, 6, 9, 10, 0), null),
        new Incident("INC-009", "로그인 불가 장애", "OAuth 토큰 갱신 실패", Incident.Status.OPEN, Incident.Severity.CRITICAL, "auth", LocalDateTime.of(2026, 6, 10, 8, 0), null),
        new Incident("INC-010", "파일 업로드 실패", "GCS 버킷 권한 오류", Incident.Status.INVESTIGATING, Incident.Severity.LOW, "storage", LocalDateTime.of(2026, 6, 10, 9, 0), null)
    );

    public List<Incident> getIncidents(String status, String severity, LocalDate startDate, LocalDate endDate) {
        return MOCK_DATA.stream()
            .filter(i -> status == null || i.getStatus().name().equalsIgnoreCase(status))
            .filter(i -> severity == null || i.getSeverity().name().equalsIgnoreCase(severity))
            .filter(i -> startDate == null || !i.getOccurredAt().toLocalDate().isBefore(startDate))
            .filter(i -> endDate == null || !i.getOccurredAt().toLocalDate().isAfter(endDate))
            .collect(Collectors.toList());
    }

    public List<Incident> getAllIncidents() {
        return MOCK_DATA;
    }

    public List<Incident> getHistory(LocalDate startDate, LocalDate endDate, String service) {
        return MOCK_DATA.stream()
            .filter(i -> i.getStatus() == Incident.Status.RESOLVED || i.getStatus() == Incident.Status.CLOSED)
            .filter(i -> service == null || i.getService().equalsIgnoreCase(service))
            .filter(i -> startDate == null || !i.getOccurredAt().toLocalDate().isBefore(startDate))
            .filter(i -> endDate == null || !i.getOccurredAt().toLocalDate().isAfter(endDate))
            .collect(Collectors.toList());
    }

    public IncidentStats getStats(LocalDate startDate, LocalDate endDate) {
        List<Incident> filtered = MOCK_DATA.stream()
            .filter(i -> startDate == null || !i.getOccurredAt().toLocalDate().isBefore(startDate))
            .filter(i -> endDate == null || !i.getOccurredAt().toLocalDate().isAfter(endDate))
            .collect(Collectors.toList());

        long total = filtered.size();
        long critical = filtered.stream().filter(i -> i.getSeverity() == Incident.Severity.CRITICAL).count();
        long high = filtered.stream().filter(i -> i.getSeverity() == Incident.Severity.HIGH).count();
        long medium = filtered.stream().filter(i -> i.getSeverity() == Incident.Severity.MEDIUM).count();
        long low = filtered.stream().filter(i -> i.getSeverity() == Incident.Severity.LOW).count();

        double mttr = filtered.stream()
            .filter(i -> i.getMttrMinutes() != null)
            .mapToLong(Incident::getMttrMinutes)
            .average()
            .orElse(0);

        // 동일 서비스에서 2회 이상 발생한 장애를 재발로 간주
        long recurrent = filtered.stream()
            .collect(Collectors.groupingBy(Incident::getService, Collectors.counting()))
            .values().stream()
            .filter(c -> c > 1)
            .mapToLong(c -> c - 1)
            .sum();
        double recurrenceRate = total == 0 ? 0 : Math.round((double) recurrent / total * 1000) / 10.0;

        return new IncidentStats(total, critical, high, medium, low, Math.round(mttr * 10) / 10.0, recurrenceRate);
    }
}
