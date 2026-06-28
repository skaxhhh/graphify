package com.graphify.admin;

import com.graphify.common.dto.ApiResponse;
import com.graphify.market.Kospi200SeedService;
import com.graphify.market.MarketIngestionJobRunner;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 시장데이터 운영 엔드포인트 (ROLE_ADMIN, SecurityConfig 보호).
 * 빈 운영 DB 부트스트랩용: KOSPI200 마스터 시드 + 일봉 적재 트리거.
 */
@RestController
@RequestMapping("/api/v1/admin/market")
public class AdminMarketController {

    private final Kospi200SeedService kospi200SeedService;
    private final MarketIngestionJobRunner ingestionJobRunner;

    public AdminMarketController(
            Kospi200SeedService kospi200SeedService,
            MarketIngestionJobRunner ingestionJobRunner) {
        this.kospi200SeedService = kospi200SeedService;
        this.ingestionJobRunner = ingestionJobRunner;
    }

    /** companies 마스터에 KOSPI200 종목을 ticker 기준 UPSERT. 멱등. */
    @PostMapping("/seed-kospi200")
    public ApiResponse<Kospi200SeedService.SeedResult> seedKospi200() {
        return ApiResponse.ok(kospi200SeedService.seed());
    }

    /**
     * in_kospi200=true 종목 일봉 적재를 백그라운드로 시작한다(fire-and-forget).
     * 99종목 직렬 외부호출은 수 분이 걸려 동기 응답 시 게이트웨이 타임아웃 → CORS 마스킹이 발생하므로,
     * 즉시 202(STARTED)를 반환하고 진행은 상태 조회 엔드포인트로 확인한다.
     * 이미 실행 중이면 409(ALREADY_RUNNING).
     */
    @PostMapping("/ingest-kospi200")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestKospi200() {
        boolean started = ingestionJobRunner.tryStartKospi200DailyIngest();
        if (!started) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.ok(Map.of("status", "ALREADY_RUNNING")));
        }
        return ResponseEntity.accepted()
                .body(ApiResponse.ok(Map.of("status", "STARTED")));
    }

    /** 최근(또는 진행 중) 일봉 적재 작업 상태. */
    @GetMapping("/ingest-kospi200/status")
    public ApiResponse<MarketIngestionJobRunner.JobStatus> ingestKospi200Status() {
        return ApiResponse.ok(ingestionJobRunner.status());
    }
}
