package com.graphify.market;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
class MarketBarIntradayRepositoryMaxTsTest {

    @Autowired
    MarketBarIntradayRepository repository;

    @Test
    void findMaxTs_returns_latest_bar_timestamp() {
        Instant t1 = Instant.now().minus(20, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        Instant t2 = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        repository.save(new MarketBarIntraday("A005930", t1, "5m", 70000.0, 71000.0, 69000.0, 70500.0, 1000L, "YAHOO"));
        repository.save(new MarketBarIntraday("A005930", t2, "5m", 70500.0, 72000.0, 70000.0, 71500.0, 1200L, "YAHOO"));

        Optional<Instant> maxTs = repository.findMaxTsBySymbolAndInterval("A005930", "5m");

        assertThat(maxTs).isPresent().contains(t2);
    }

    @Test
    void findMaxTs_returns_empty_when_no_bars() {
        Optional<Instant> maxTs = repository.findMaxTsBySymbolAndInterval("A000000", "5m");
        assertThat(maxTs).isEmpty();
    }
}
