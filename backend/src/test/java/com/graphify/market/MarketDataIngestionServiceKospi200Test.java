package com.graphify.market;

import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.company.market.YahooFinanceChartClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.trading.rule.TradingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * DATA-02 단위 테스트: ingestDailyForKospi200()
 *
 * ingestDaily()는 외부 Yahoo API를 호출하므로 Spy + doReturn으로 stub.
 * companyRepository.findByInKospi200True() 반환값을 제어해 필터링 동작 검증.
 */
@ExtendWith(MockitoExtension.class)
class MarketDataIngestionServiceKospi200Test {

    @Mock
    private TradingRuleRepository ruleRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private YahooFinanceChartClient yahooClient;

    @Mock
    private MarketBarRepository barRepository;

    @Mock
    private MarketBarIntradayRepository intradayRepository;

    @Mock
    private ObjectMapper objectMapper;

    private MarketDataIngestionService service;

    @BeforeEach
    void setUp() {
        service = new MarketDataIngestionService(
                ruleRepository,
                companyRepository,
                yahooClient,
                barRepository,
                intradayRepository,
                objectMapper
        );
    }

    // -----------------------------------------------------------------------
    // 헬퍼: Company 생성
    // -----------------------------------------------------------------------

    private Company companyWithTicker(String ticker) {
        Company c = new Company();
        c.setTicker(ticker);
        c.setInKospi200(true);
        return c;
    }

    private Company companyWithNullTicker() {
        Company c = new Company();
        c.setTicker(null);
        c.setInKospi200(true);
        return c;
    }

    // -----------------------------------------------------------------------
    // Test 1: findByInKospi200True()가 2개 종목 반환 시 ingestDaily()가 2번 호출된다
    // -----------------------------------------------------------------------

    @Test
    void ingestDailyForKospi200_callsIngestDailyForEachCompany() {
        // given
        Company c1 = companyWithTicker("005930");
        Company c2 = companyWithTicker("000660");
        when(companyRepository.findByInKospi200True()).thenReturn(List.of(c1, c2));
        when(companyRepository.findByTicker(anyString())).thenReturn(Optional.empty());
        when(yahooClient.fetchDailyOhlcv(anyString())).thenReturn(List.of());

        // when
        service.ingestDailyForKospi200();

        // then — ingestDaily가 내부적으로 companyRepository.findByTicker를 호출하므로 2번 호출됨
        verify(companyRepository, times(2)).findByTicker(anyString());
    }

    // -----------------------------------------------------------------------
    // Test 2: ticker=null인 Company는 ingestDaily() 호출 없이 건너뛰어진다
    // -----------------------------------------------------------------------

    @Test
    void ingestDailyForKospi200_skipsCompanyWithNullTicker() {
        // given
        Company nullTickerCompany = companyWithNullTicker();
        when(companyRepository.findByInKospi200True()).thenReturn(List.of(nullTickerCompany));

        // when
        service.ingestDailyForKospi200();

        // then — ingestDaily는 companyRepository.findByTicker를 호출하므로 전혀 호출되지 않아야 함
        verify(companyRepository, never()).findByTicker(anyString());
        verify(yahooClient, never()).fetchDailyOhlcv(anyString());
    }

    // -----------------------------------------------------------------------
    // Test 3: findByInKospi200True()가 빈 리스트 반환 시 반환값 0, ingestDaily() 미호출
    // -----------------------------------------------------------------------

    @Test
    void ingestDailyForKospi200_returnsZeroWhenNoKospi200Companies() {
        // given
        when(companyRepository.findByInKospi200True()).thenReturn(List.of());

        // when
        int result = service.ingestDailyForKospi200();

        // then
        assertThat(result).isEqualTo(0);
        verify(yahooClient, never()).fetchDailyOhlcv(anyString());
    }

    // -----------------------------------------------------------------------
    // Test 4: ingestDaily()가 >0 반환 시 결과 카운트에 포함된다
    // -----------------------------------------------------------------------

    @Test
    void ingestDailyForKospi200_countsSymbolsWithPositiveIngestion() {
        // given — 2개 종목, Yahoo가 각각 1개 bar 반환하도록 stub
        Company c1 = companyWithTicker("005930");
        Company c2 = companyWithTicker("000660");
        when(companyRepository.findByInKospi200True()).thenReturn(List.of(c1, c2));

        // ingestDaily()가 내부적으로 resolveYahoo -> companyRepository.findByTicker -> YahooSymbolResolver 사용
        // YahooSymbolResolver.resolve()가 empty를 반환하면 ingestDaily()가 0을 반환.
        // 테스트는 ingestDailyForKospi200()의 count 집계 로직을 직접 검증해야 하므로
        // Spy 방식으로 ingestDailyForKospi200()만 실제 실행하고 ingestDaily()를 stub한다.
        MarketDataIngestionService spyService = spy(service);
        doReturn(5).when(spyService).ingestDaily("005930");
        doReturn(3).when(spyService).ingestDaily("000660");

        // when
        int result = spyService.ingestDailyForKospi200();

        // then
        assertThat(result).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // Test 5: ingestDaily()가 0 반환 시 카운트에 포함되지 않는다
    // -----------------------------------------------------------------------

    @Test
    void ingestDailyForKospi200_doesNotCountSymbolsWithZeroIngestion() {
        // given — 2개 종목, 둘 다 ingestDaily()가 0 반환
        Company c1 = companyWithTicker("005930");
        Company c2 = companyWithTicker("000660");
        when(companyRepository.findByInKospi200True()).thenReturn(List.of(c1, c2));

        MarketDataIngestionService spyService = spy(service);
        doReturn(0).when(spyService).ingestDaily("005930");
        doReturn(0).when(spyService).ingestDaily("000660");

        // when
        int result = spyService.ingestDailyForKospi200();

        // then
        assertThat(result).isEqualTo(0);
    }
}
