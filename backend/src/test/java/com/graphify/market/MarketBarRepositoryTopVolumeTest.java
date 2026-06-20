package com.graphify.market;

import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DATA-04: findTopVolumeSymbolsOnDate() — in_kospi200=true 종목 중 거래량 상위 N 쿼리 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MarketBarRepositoryTopVolumeTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MarketBarRepository barRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company saveKospi200Company(String ticker) {
        Company c = new Company();
        c.setName("Test " + ticker);
        c.setTicker(ticker);
        c.setMarket("KOSPI");
        c.setInKospi200(true);
        return em.persistAndFlush(c);
    }

    private Company saveNonKospi200Company(String ticker) {
        Company c = new Company();
        c.setName("NonKospi " + ticker);
        c.setTicker(ticker);
        c.setMarket("KOSPI");
        c.setInKospi200(false);
        return em.persistAndFlush(c);
    }

    private MarketBar saveBar(String ticker, LocalDate date, Long volume) {
        MarketBar bar = new MarketBar(ticker, date, 10.0, 11.0, 9.0, 10.0, volume, "TEST");
        return em.persistAndFlush(bar);
    }

    // Test 1: 거래량 내림차순 상위 N 종목 반환
    @Test
    void findTopVolumeSymbolsOnDate_returnsTopNByVolumeDesc() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        saveKospi200Company("AAA");
        saveKospi200Company("BBB");
        saveKospi200Company("CCC");
        saveKospi200Company("DDD");
        saveKospi200Company("EEE");

        saveBar("AAA", date, 100L);
        saveBar("BBB", date, 300L);
        saveBar("CCC", date, 200L);
        saveBar("DDD", date, 400L);
        saveBar("EEE", date, 150L);

        List<String> result = barRepository.findTopVolumeSymbolsOnDate(date, PageRequest.of(0, 3));

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("DDD", "BBB", "CCC");
    }

    // Test 2: in_kospi200=false 종목은 결과에 포함되지 않는다
    @Test
    void findTopVolumeSymbolsOnDate_excludesNonKospi200() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        saveKospi200Company("KOSPI_A");
        saveNonKospi200Company("NON_A");

        saveBar("KOSPI_A", date, 100L);
        saveBar("NON_A", date, 999L);  // volume 높지만 in_kospi200=false

        List<String> result = barRepository.findTopVolumeSymbolsOnDate(date, PageRequest.of(0, 10));

        assertThat(result).containsExactly("KOSPI_A");
        assertThat(result).doesNotContain("NON_A");
    }

    // Test 3: volume=null 봉은 결과에 포함되지 않는다
    @Test
    void findTopVolumeSymbolsOnDate_excludesNullVolume() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        saveKospi200Company("WITH_VOL");
        saveKospi200Company("NULL_VOL");

        saveBar("WITH_VOL", date, 500L);
        saveBar("NULL_VOL", date, null);  // volume=null

        List<String> result = barRepository.findTopVolumeSymbolsOnDate(date, PageRequest.of(0, 10));

        assertThat(result).containsExactly("WITH_VOL");
        assertThat(result).doesNotContain("NULL_VOL");
    }

    // Test 4: 다른 날짜의 데이터는 지정 날짜 쿼리에 포함되지 않는다
    @Test
    void findTopVolumeSymbolsOnDate_excludesOtherDates() {
        LocalDate targetDate = LocalDate.of(2024, 1, 15);
        LocalDate otherDate = LocalDate.of(2024, 1, 16);

        saveKospi200Company("TICKER_X");
        saveKospi200Company("TICKER_Y");

        saveBar("TICKER_X", targetDate, 100L);
        saveBar("TICKER_Y", otherDate, 999L);  // 다른 날짜

        List<String> result = barRepository.findTopVolumeSymbolsOnDate(targetDate, PageRequest.of(0, 10));

        assertThat(result).containsExactly("TICKER_X");
        assertThat(result).doesNotContain("TICKER_Y");
    }
}
