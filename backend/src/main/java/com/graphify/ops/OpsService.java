package com.graphify.ops;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpsService {

    private final List<BatchJob> JOBS = new ArrayList<>(List.of(
        new BatchJob("JOB-001", "일일 매출 집계", "당일 전체 채널 매출 합산 처리", BatchJob.Status.SUCCESS, "sales", LocalDateTime.of(2026, 6, 10, 1, 0), LocalDateTime.of(2026, 6, 10, 1, 12), null, 0),
        new BatchJob("JOB-002", "정산 데이터 생성", "파트너사 정산 데이터 생성 및 전송", BatchJob.Status.FAILED, "settlement", LocalDateTime.of(2026, 6, 10, 1, 30), LocalDateTime.of(2026, 6, 10, 1, 35), "DB 커넥션 타임아웃 (30s 초과)", 1),
        new BatchJob("JOB-003", "재고 현황 동기화", "ERP ↔ 영업시스템 재고 수량 동기화", BatchJob.Status.SUCCESS, "inventory", LocalDateTime.of(2026, 6, 10, 2, 0), LocalDateTime.of(2026, 6, 10, 2, 8), null, 0),
        new BatchJob("JOB-004", "미결 승인 건 알림", "24시간 이상 미결 승인 건 담당자 알림 발송", BatchJob.Status.FAILED, "approval", LocalDateTime.of(2026, 6, 10, 7, 0), LocalDateTime.of(2026, 6, 10, 7, 2), "SMTP 서버 연결 실패", 2),
        new BatchJob("JOB-005", "전일 리포트 생성", "전일자 영업 실적 리포트 PDF 생성", BatchJob.Status.SUCCESS, "report", LocalDateTime.of(2026, 6, 10, 6, 0), LocalDateTime.of(2026, 6, 10, 6, 20), null, 0),
        new BatchJob("JOB-006", "고객 포인트 만료 처리", "만료 예정 포인트 소멸 처리", BatchJob.Status.RUNNING, "crm", LocalDateTime.of(2026, 6, 10, 9, 0), null, null, 0),
        new BatchJob("JOB-007", "세금계산서 발행", "전일 확정 거래 세금계산서 자동 발행", BatchJob.Status.FAILED, "tax", LocalDateTime.of(2026, 6, 10, 3, 0), LocalDateTime.of(2026, 6, 10, 3, 5), "국세청 API 응답 없음 (503)", 0),
        new BatchJob("JOB-008", "채널별 실적 분배", "온라인/오프라인/파트너 채널 실적 분배 계산", BatchJob.Status.SUCCESS, "sales", LocalDateTime.of(2026, 6, 10, 2, 30), LocalDateTime.of(2026, 6, 10, 2, 45), null, 0),
        new BatchJob("JOB-009", "미수금 알림 발송", "30일 이상 미수금 거래처 알림", BatchJob.Status.PENDING, "finance", null, null, null, 0),
        new BatchJob("JOB-010", "상품 가격 업데이트", "협력사 가격 정책 자동 반영", BatchJob.Status.SUCCESS, "catalog", LocalDateTime.of(2026, 6, 10, 0, 30), LocalDateTime.of(2026, 6, 10, 0, 42), null, 0)
    ));

    public List<BatchJob> getAllJobs() {
        return JOBS;
    }

    public List<BatchJob> getFailedJobs() {
        return JOBS.stream()
            .filter(j -> j.getStatus() == BatchJob.Status.FAILED)
            .collect(Collectors.toList());
    }

    public BatchJob retryJob(String id) {
        return JOBS.stream()
            .filter(j -> j.getId().equals(id))
            .findFirst()
            .map(job -> {
                job.setStatus(BatchJob.Status.RUNNING);
                job.setRetryCount(job.getRetryCount() + 1);
                job.setStartedAt(LocalDateTime.now());
                job.setFinishedAt(null);
                job.setErrorMessage(null);
                return job;
            })
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }

    public ClosingReport getTodayClosing() {
        LocalDate today = LocalDate.now();
        List<ClosingReport.ClosingItem> items = List.of(
            new ClosingReport.ClosingItem("매출 집계 완료 여부", "SALES", "OK", "금일 전체 채널 매출 집계 정상 완료", "₩ 1,284,500,000"),
            new ClosingReport.ClosingItem("채널별 실적 분배", "SALES", "OK", "온라인 62% / 오프라인 28% / 파트너 10%", null),
            new ClosingReport.ClosingItem("정산 데이터 생성", "SETTLEMENT", "FAILED", "DB 커넥션 타임아웃으로 정산 배치 실패 - 재처리 필요", null),
            new ClosingReport.ClosingItem("세금계산서 발행", "SETTLEMENT", "FAILED", "국세청 API 장애로 발행 지연 - 수동 발행 필요", null),
            new ClosingReport.ClosingItem("재고 현황 동기화", "INVENTORY", "OK", "ERP 재고 수량 동기화 완료 (총 4,821 SKU)", "4,821 SKU"),
            new ClosingReport.ClosingItem("재고 부족 상품", "INVENTORY", "WARNING", "안전재고 미달 상품 13건 - 발주 검토 필요", "13건"),
            new ClosingReport.ClosingItem("미결 승인 건", "APPROVAL", "WARNING", "24시간 이상 미결 승인 8건 존재 (알림 발송 실패)", "8건"),
            new ClosingReport.ClosingItem("미수금 현황", "APPROVAL", "PENDING", "미수금 알림 배치 미실행 (대기 중)", null)
        );
        return new ClosingReport(today, "WARNING", items, "정산 배치 및 세금계산서 발행 실패 2건 조치 필요. 재고 부족 및 미결 승인 건 확인 요망.");
    }

    public ClosingReport getYesterdayClosing() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<ClosingReport.ClosingItem> items = List.of(
            new ClosingReport.ClosingItem("매출 집계 완료 여부", "SALES", "OK", "전일 전체 채널 매출 집계 정상 완료", "₩ 987,300,000"),
            new ClosingReport.ClosingItem("채널별 실적 분배", "SALES", "OK", "온라인 58% / 오프라인 31% / 파트너 11%", null),
            new ClosingReport.ClosingItem("정산 데이터 생성", "SETTLEMENT", "OK", "파트너사 27개사 정산 데이터 전송 완료", "27개사"),
            new ClosingReport.ClosingItem("세금계산서 발행", "SETTLEMENT", "OK", "거래 건 342건 세금계산서 자동 발행 완료", "342건"),
            new ClosingReport.ClosingItem("재고 현황 동기화", "INVENTORY", "OK", "ERP 재고 동기화 완료", null),
            new ClosingReport.ClosingItem("재고 부족 상품", "INVENTORY", "OK", "안전재고 미달 상품 없음", "0건"),
            new ClosingReport.ClosingItem("미결 승인 건", "APPROVAL", "OK", "전일 미결 승인 건 전량 처리 완료", "0건"),
            new ClosingReport.ClosingItem("미수금 현황", "APPROVAL", "WARNING", "30일 이상 미수금 거래처 5곳 알림 발송 완료", "5건")
        );
        return new ClosingReport(yesterday, "OK", items, "전일 마감 정상 완료. 미수금 거래처 5곳 알림 발송 처리됨.");
    }
}
