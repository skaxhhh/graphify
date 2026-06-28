package com.graphify.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * v1.6.0 단위 테스트: Kospi200SeedService.seed()
 *
 * CompanyRepository를 ticker→Company HashMap으로 백킹해 멱등성(2회 실행 시 update)과
 * market/instrumentType/inKospi200 플래그 설정을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class Kospi200SeedServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    private Kospi200SeedService service;
    private Map<String, Company> store;

    @BeforeEach
    void setUp() {
        store = new HashMap<>();
        when(companyRepository.findFirstByTickerOrderByIdAsc(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(inv -> {
                    Company c = inv.getArgument(0);
                    store.put(c.getTicker(), c);
                    return c;
                });
        service = new Kospi200SeedService(companyRepository);
    }

    @Test
    void seed_firstRun_insertsAllRows() {
        Kospi200SeedService.SeedResult result = service.seed();

        assertThat(result.inserted()).isGreaterThan(0);
        assertThat(result.updated()).isEqualTo(0);
        assertThat(result.total()).isEqualTo(result.inserted());
        assertThat(store).hasSize(result.inserted());
    }

    @Test
    void seed_isIdempotent_secondRunUpdatesAll() {
        Kospi200SeedService.SeedResult first = service.seed();
        Kospi200SeedService.SeedResult second = service.seed();

        // 2회차는 모두 기존 ticker → update만, insert 0
        assertThat(second.inserted()).isEqualTo(0);
        assertThat(second.updated()).isEqualTo(first.inserted());
        assertThat(second.total()).isEqualTo(first.inserted());
        assertThat(store).hasSize(first.inserted());
    }

    @Test
    void seed_setsKospi200Flags() {
        service.seed();

        // 대표 종목(삼성전자)에 플래그가 모두 설정됐는지 확인
        Company samsung = store.get("005930");
        assertThat(samsung).isNotNull();
        assertThat(samsung.getMarket()).isEqualTo("KOSPI");
        assertThat(samsung.getInstrumentType()).isEqualTo("COMMON_STOCK");
        assertThat(samsung.isInKospi200()).isTrue();
        assertThat(samsung.getName()).isNotBlank();
    }

    @Test
    void seed_existingCompany_preservesNameAndSetsFlags() {
        // 기존 회사가 이름을 이미 가지고 있으면 보존, 플래그는 갱신
        Company existing = new Company();
        existing.setTicker("005930");
        existing.setName("삼성전자 (기존)");
        existing.setMarket("KOSDAQ");
        existing.setInstrumentType("ETF");
        existing.setInKospi200(false);
        store.put("005930", existing);

        service.seed();

        Company updated = store.get("005930");
        assertThat(updated.getName()).isEqualTo("삼성전자 (기존)"); // 비어있지 않으므로 보존
        assertThat(updated.getMarket()).isEqualTo("KOSPI");        // 항상 갱신
        assertThat(updated.getInstrumentType()).isEqualTo("COMMON_STOCK");
        assertThat(updated.isInKospi200()).isTrue();
    }
}
