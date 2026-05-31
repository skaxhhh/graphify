package com.graphify.company.market;

import static org.assertj.core.api.Assertions.assertThat;

import com.graphify.company.dto.CompanyMarketTechnicalDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarketTechnicalContextFormatterTest {

    @Test
    void format_includesPriceRsiAndAlignment() {
        CompanyMarketTechnicalDto dto = new CompanyMarketTechnicalDto(
                "005930.KS",
                "KRW",
                299500.0,
                2.15,
                292500.0,
                298000.0,
                290000.0,
                270000.0,
                250000.0,
                230000.0,
                58.3,
                MaAlignment.BULLISH_ALIGN,
                true,
                true,
                Instant.parse("2026-05-26T06:19:59Z"),
                "2026-05-26",
                "LIVE",
                "현재가",
                Instant.parse("2026-05-26T06:20:00Z"),
                "NAVER",
                "YAHOO"
        );
        String text = MarketTechnicalContextFormatter.format(dto);
        assertThat(text).contains("시장·기술 지표");
        assertThat(text).contains("299,500");
        assertThat(text).contains("RSI(14)");
        assertThat(text).contains("정배열");
    }
}
