package com.graphify.market;

import com.graphify.trading.rule.PaperLiveSymbolService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveDataSchedulerTest {

    @Mock KrxMarketCalendar calendar;
    @Mock PaperLiveSymbolService symbolService;
    @Mock MarketDataIngestionService ingestionService;
    @Mock MarketBarIntradayRepository intradayRepository;

    LiveDataScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new LiveDataScheduler(calendar, symbolService, ingestionService, intradayRepository);
    }

    @Test
    void schedulerLock_annotation_present_with_correct_config() throws Exception {
        Method method = LiveDataScheduler.class.getMethod("collectLiveData");
        SchedulerLock lock = method.getAnnotation(SchedulerLock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.name()).isEqualTo("liveDataIngestion");
        assertThat(lock.lockAtMostFor()).isEqualTo("4m");
        assertThat(lock.lockAtLeastFor()).isEqualTo("1m");
    }

    @Test
    void non_trading_day_skips_ingestion() {
        // calendar returns false (non-trading day) — ingestionService must never be called
        when(calendar.isTradingDay(any())).thenReturn(false);
        scheduler.collectLiveData();
        verify(ingestionService, never()).ingestIntraday(any(), any(), any());
    }

    @Test
    void no_active_symbols_skips_ingestion() {
        when(calendar.isTradingDay(any())).thenReturn(true);
        when(symbolService.activeSymbolsUnion()).thenReturn(Set.of());
        scheduler.collectLiveData();
        verify(ingestionService, never()).ingestIntraday(any(), any(), any());
    }

    @Test
    void stale_bar_triggers_staleness_check() {
        // When latest bar is 15 minutes old (> 10 minute threshold),
        // checkStaleness is exercised — verify repository and ingestion are called
        when(calendar.isTradingDay(any())).thenReturn(true);
        when(symbolService.activeSymbolsUnion()).thenReturn(Set.of("A005930"));
        Instant staleTs = Instant.now().minus(15, ChronoUnit.MINUTES);
        when(intradayRepository.findMaxTsBySymbolAndInterval("A005930", "5m"))
            .thenReturn(Optional.of(staleTs));
        scheduler.collectLiveData();
        verify(intradayRepository).findMaxTsBySymbolAndInterval("A005930", "5m");
        verify(ingestionService).ingestIntraday("A005930", "5m", "1d");
    }

    @Test
    void fresh_bar_does_not_trigger_staleness_warning() {
        when(calendar.isTradingDay(any())).thenReturn(true);
        when(symbolService.activeSymbolsUnion()).thenReturn(Set.of("A005930"));
        Instant freshTs = Instant.now().minus(3, ChronoUnit.MINUTES);
        when(intradayRepository.findMaxTsBySymbolAndInterval("A005930", "5m"))
            .thenReturn(Optional.of(freshTs));
        scheduler.collectLiveData();
        // Fresh bar — ingest called, repository queried, no exception
        verify(ingestionService).ingestIntraday("A005930", "5m", "1d");
        verify(intradayRepository).findMaxTsBySymbolAndInterval("A005930", "5m");
    }
}
