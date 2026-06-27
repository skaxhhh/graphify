package com.graphify.admin;

import com.graphify.common.dto.ApiResponse;
import com.graphify.market.Kospi200SeedService;
import com.graphify.market.MarketDataIngestionService;
import java.util.Map;
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

    /** in_kospi200=true 종목의 일봉을 적재. 적재된 종목 수 반환. */
    @PostMapping("/ingest-kospi200")
    public ApiResponse<Map<String, Object>> ingestKospi200() {
        int symbols = ingestionService.ingestDailyForKospi200();
        return ApiResponse.ok(Map.of("symbols", symbols));
    }
}
