package com.graphify.trading.engine;

import java.time.LocalDate;
import java.util.List;

/**
 * 엔진이 시세를 조회하는 추상 포트. 백테스트는 과거 봉, 포워드는 최신 봉을 공급.
 */
public interface MarketDataPort {

    /** 종목(티커)의 일봉 시계열(오래된 → 최신). 조회 실패 시 빈 리스트. */
    List<Bar> historicalDailyBars(String symbol);

    /**
     * 지정 날짜에 in_kospi200=true 종목 중 거래량 상위 topN 종목 티커 반환.
     * Look-ahead bias 방지: 해당 날짜의 데이터만 사용.
     */
    default List<String> topVolumeSymbols(LocalDate date, int topN) {
        return List.of();
    }

    /**
     * 지정 시장의 in_kospi200=true 종목 전체 티커 반환.
     * volume_top_n 백테스트 시 데이터 사전 로드에 사용.
     */
    default List<String> symbolsByMarket(String market) {
        return List.of();
    }
}
