package com.graphify.admin;

import com.graphify.common.dto.ApiResponse;
import com.graphify.market.Kospi200SeedService;
import com.graphify.market.MarketDataIngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 시장데이터 운영 엔드포인트 (ROLE_ADMIN, SecurityConfig 보호).
 * 빈 운영 DB 부트스트랩용: KOSPI200 마스터 시드 + 일봉 적재 트리거.
 */
@RestController
@RequestMapping("/api/v1/admin/market")
public class AdminMarketController {

    private final Kospi200SeedService kospi200SeedService;
    private final MarketDataIngestionService ingestionService;

    public AdminMarketController(
            Kospi200SeedService kospi200SeedService,
            MarketDataIngestionService ingestionService) {
        this.kospi200SeedService = kospi200SeedService;
        this.ingestionService = ingestionService;
    }

    /** companies 마스터에 KOSPI200 종목을 ticker 기준 UPSERT. 멱등. */
    @PostMapping("/seed-kospi200")
    public ApiResponse<Kospi200SeedService.SeedResult> seedKospi200() {
        return ApiResponse.ok(kospi200SeedService.seed());
    }

    /**
     * KOSPI200 일봉을 청크 단위로 적재한다. 99종목 직렬 외부호출(수 분)을 한 요청에서 처리하면
     * 게이트웨이 타임아웃 → CORS 마스킹이 발생하므로, 프론트가 size개씩 nextOffset으로 순회한다.
     * 각 청크는 독립 HTTP 요청이라 Cloud Run 기본 CPU 정책에서도 안전하게 완주한다.
     */
    @PostMapping("/ingest-kospi200/batch")
    public ApiResponse<MarketDataIngestionService.BatchResult> ingestKospi200Batch(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(ingestionService.ingestDailyForKospi200Batch(offset, size));
    }
}
