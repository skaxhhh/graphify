package com.graphify.market.volume;

import com.graphify.market.MarketBarIntradayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DATA-06-SC2 and DATA-06-SC3:
 * YahooCumulativeVolumeAdapter — Yahoo 5m intraday cumulative volume ranking.
 *
 * <p>SC2: instrument_type=COMMON_STOCK 필터로 ETF/ETN/우선주/SPAC 제외 (JPQL + V36 컬럼).
 * SC3: ~1분 TTL 캐시 — 동일 윈도우 2번째 호출은 repository 재호출 없이 캐시 반환.</p>
 *
 * <p>Mock 전략: MarketBarIntradayRepository.findCumulativeVolumeByMarketAndDate(market, dayStart, dayEnd, pageable)
 * 를 Mockito로 stub. Instant 범위 매처는 any()로 처리해 KST 경계 계산을 분리.</p>
 */
@ExtendWith(MockitoExtension.class)
class YahooCumulativeVolumeAdapterTest {

    @Mock
    private MarketBarIntradayRepository intradayRepository;

    private YahooCumulativeVolumeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new YahooCumulativeVolumeAdapter(intradayRepository);
    }

    // -------------------------------------------------------------------------
    // Test 1 (DATA-06-SC2): 보통주 3종목 mock 응답 →
    //   topVolume("KOSPI", today, 3, true) 가 3 티커 그대로 순서 유지 반환.
    //   JPQL에서 이미 COMMON_STOCK 필터 + SUM DESC 처리됨 (ETF/우선주 없음).
    // -------------------------------------------------------------------------
    @Test
    void topVolume_returnsOnlyCommonStocksInVolumeDescOrder() {
        LocalDate today = LocalDate.now();
        when(intradayRepository.findCumulativeVolumeByMarketAndDate(
                eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of("005930", "000660", "035420"));

        List<String> result = adapter.topVolume("KOSPI", today, 3, true);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("005930", "000660", "035420");
    }

    // -------------------------------------------------------------------------
    // Test 2 (DATA-06-SC2): excludeEtf=false でも 동작, 예외 없음
    //   (JPQL은 항상 COMMON_STOCK 필터 — excludeEtf는 의미상 메모이므로 무시)
    // -------------------------------------------------------------------------
    @Test
    void topVolume_withExcludeEtfFalse_stillReturnsWhatRepoProvides() {
        LocalDate today = LocalDate.now();
        when(intradayRepository.findCumulativeVolumeByMarketAndDate(
                eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of("A", "B"));

        List<String> result = adapter.topVolume("KOSPI", today, 5, false);

        assertThat(result).containsExactly("A", "B");
    }

    // -------------------------------------------------------------------------
    // Test 3: repository 빈 리스트 반환 → 빈 리스트 반환, NPE 없음
    // -------------------------------------------------------------------------
    @Test
    void topVolume_whenRepositoryReturnsEmpty_returnsEmptyList() {
        LocalDate today = LocalDate.now();
        when(intradayRepository.findCumulativeVolumeByMarketAndDate(
                eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        List<String> result = adapter.topVolume("KOSPI", today, 10, true);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test 3b: repository 예외 발생 → 빈 리스트 반환, 예외 전파 없음 (NPE 없음)
    // -------------------------------------------------------------------------
    @Test
    void topVolume_whenRepositoryThrows_returnsEmptyListWithoutPropagation() {
        LocalDate today = LocalDate.now();
        when(intradayRepository.findCumulativeVolumeByMarketAndDate(
                eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("DB connection error"));

        assertThatNoException().isThrownBy(
                () -> adapter.topVolume("KOSPI", today, 10, true));

        List<String> result = adapter.topVolume("KOSPI", today, 10, true);
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test 4: topN > 가용 종목 수 → 가용 전체만 반환 (subList IndexOutOfBounds 없음)
    // -------------------------------------------------------------------------
    @Test
    void topVolume_whenTopNExceedsAvailable_returnsAllAvailable() {
        LocalDate today = LocalDate.now();
        when(intradayRepository.findCumulativeVolumeByMarketAndDate(
                eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of("A", "B"));

        List<String> result = adapter.topVolume("KOSPI", today, 100, true);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("A", "B");
    }

    // -------------------------------------------------------------------------
    // Test 5 (DATA-06-SC3): 동일 윈도우 2회 호출 → repository 1회만 호출
    //   2번째는 캐시 반환 (verify mock 호출 횟수 = 1)
    // -------------------------------------------------------------------------
    @Test
    void topVolume_secondCallWithinTtlWindow_returnsCachedResult_withoutRepositoryCall() {
        LocalDate today = LocalDate.now();
        when(intradayRepository.findCumulativeVolumeByMarketAndDate(
                eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of("005930", "000660"));

        // First call — populates cache
        List<String> first = adapter.topVolume("KOSPI", today, 2, true);
        // Second call — same adapter instance, within TTL (no time elapsed in test)
        List<String> second = adapter.topVolume("KOSPI", today, 2, true);

        assertThat(first).containsExactly("005930", "000660");
        assertThat(second).containsExactly("005930", "000660");

        // Repository must be called exactly once (cache hit on second call)
        verify(intradayRepository, times(1))
                .findCumulativeVolumeByMarketAndDate(
                        eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // Test 6: 빈 fetch는 캐시 미갱신 → 다음 호출이 repository 재시도
    // -------------------------------------------------------------------------
    @Test
    void topVolume_whenFetchReturnsEmpty_doesNotUpdateCache_nextCallRetries() {
        LocalDate today = LocalDate.now();
        // First call: empty (cache NOT updated)
        // Second call: real data available (retried because cache.isEmpty())
        when(intradayRepository.findCumulativeVolumeByMarketAndDate(
                eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of())
                .thenReturn(List.of("005930"));

        List<String> first = adapter.topVolume("KOSPI", today, 5, true);
        assertThat(first).isEmpty();

        // Cache not set (empty result) → second call must retry repository
        List<String> second = adapter.topVolume("KOSPI", today, 5, true);
        assertThat(second).containsExactly("005930");

        // Repository called twice — empty result did not set cache
        verify(intradayRepository, times(2))
                .findCumulativeVolumeByMarketAndDate(
                        eq("KOSPI"), any(Instant.class), any(Instant.class), any(Pageable.class));
    }
}
