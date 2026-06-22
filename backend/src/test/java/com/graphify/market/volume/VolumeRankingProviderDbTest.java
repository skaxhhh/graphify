package com.graphify.market.volume;

import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.market.MarketBar;
import com.graphify.market.MarketBarRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DATA-06-SC1: VolumeRankingProvider (DbVolumeRankingAdapter) —
 * 날짜별 거래량 상위 N 반환, instrument_type='COMMON_STOCK' 필터, ETF/우선주 제외.
 *
 * @DataJpaTest + H2 create-drop (Flyway disabled) — RESEARCH Pitfall 6:
 * instrument_type 컬럼은 Company 엔티티 필드로 H2 자동 생성됨.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VolumeRankingProviderDbTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MarketBarRepository barRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company saveCommonStock(String ticker) {
        Company c = new Company();
        c.setName("Common " + ticker);
        c.setTicker(ticker);
        c.setMarket("KOSPI");
        c.setInKospi200(false); // NOTE: instrument_type filter, NOT in_kospi200
        c.setInstrumentType("COMMON_STOCK");
        return em.persistAndFlush(c);
    }

    private Company saveEtf(String ticker) {
        Company c = new Company();
        c.setName("ETF " + ticker);
        c.setTicker(ticker);
        c.setMarket("KOSPI");
        c.setInKospi200(false);
        c.setInstrumentType("ETF");
        return em.persistAndFlush(c);
    }

    private Company savePreferred(String ticker) {
        Company c = new Company();
        c.setName("Preferred " + ticker);
        c.setTicker(ticker);
        c.setMarket("KOSPI");
        c.setInKospi200(false);
        c.setInstrumentType("PREFERRED");
        return em.persistAndFlush(c);
    }

    private MarketBar saveBar(String ticker, LocalDate date, Long volume) {
        MarketBar bar = new MarketBar(ticker, date, 10.0, 11.0, 9.0, 10.0, volume, "TEST");
        return em.persistAndFlush(bar);
    }

    private DbVolumeRankingAdapter adapter() {
        return new DbVolumeRankingAdapter(barRepository);
    }

    /**
     * Test 1 (DATA-06-SC1): 보통주 3종목 + ETF 1종목에서 topVolume("KOSPI", date, 2, true)가
     * 거래량 상위 2 보통주 티커를 DESC 순으로 반환, ETF 제외.
     */
    @Test
    void topVolume_returnsTopNCommonStocksByVolumeDesc_excludesEtf() {
        LocalDate date = LocalDate.of(2024, 6, 20);

        saveCommonStock("AAA");
        saveCommonStock("BBB");
        saveCommonStock("CCC");
        saveEtf("ETF1");

        saveBar("AAA", date, 500L);   // 2nd
        saveBar("BBB", date, 1000L);  // 1st
        saveBar("CCC", date, 200L);   // 3rd
        saveBar("ETF1", date, 9999L); // ETF — must be excluded

        List<String> result = adapter().topVolume("KOSPI", date, 2, true);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("BBB", "AAA"); // DESC order
        assertThat(result).doesNotContain("ETF1");
    }

    /**
     * Test 2: topN이 가용 종목보다 크면 가용 전체만 반환 (IndexOutOfBounds 없음).
     */
    @Test
    void topVolume_whenTopNExceedsAvailable_returnsAllAvailable() {
        LocalDate date = LocalDate.of(2024, 6, 20);

        saveCommonStock("X1");
        saveCommonStock("X2");

        saveBar("X1", date, 300L);
        saveBar("X2", date, 100L);

        List<String> result = adapter().topVolume("KOSPI", date, 10, true);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("X1", "X2");
    }

    /**
     * Test 3: instrument_type='COMMON_STOCK'이 아닌 종목(ETF/PREFERRED)은 결과에서 제외.
     */
    @Test
    void topVolume_excludesNonCommonStockInstrumentTypes() {
        LocalDate date = LocalDate.of(2024, 6, 20);

        saveCommonStock("COMM1");
        saveEtf("ETF2");
        savePreferred("PREF1");

        saveBar("COMM1", date, 100L);
        saveBar("ETF2", date, 5000L);
        saveBar("PREF1", date, 3000L);

        List<String> result = adapter().topVolume("KOSPI", date, 10, true);

        assertThat(result).containsExactly("COMM1");
        assertThat(result).doesNotContain("ETF2", "PREF1");
    }
}
