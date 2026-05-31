package com.graphify.company.market;

import static org.assertj.core.api.Assertions.assertThat;

import com.graphify.config.GraphifyMarketProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NaverFinanceQuoteClientTest {

    @Test
    void parseHtml_extractsKrxBlindPriceAndPercent() {
        GraphifyMarketProperties props = new GraphifyMarketProperties();
        props.setNaverEnabled(true);
        NaverFinanceQuoteClient client = new NaverFinanceQuoteClient(
                props,
                RestClient.builder().baseUrl("https://finance.naver.com").build()
        );

        String html = """
                <p>2026년 05월 26일 15시 59분 기준 장마감</p>
                <div id="rate_info_krx" style="display:none">
                <dl class="blind">
                <dt>알테오젠</dt>
                <dd>오늘의시세 365,500 포인트</dd>
                <dd>1,000 포인트 상승</dd>
                <dd>0.27% 플러스</dd>
                </dl>
                </div>
                <div id="rate_info_nxt">
                <dl class="blind">
                <dd>오늘의시세 367,000 포인트</dd>
                <dd>0.69% 플러스</dd>
                </dl>
                </div>
                """;

        var quote = client.parseHtml(html);
        assertThat(quote).isPresent();
        assertThat(quote.get().price()).isEqualTo(365_500.0);
        assertThat(quote.get().changePercent()).isEqualTo(0.27);
        assertThat(quote.get().changeAmount()).isEqualTo(1_000.0);
        assertThat(quote.get().marketLabel()).isEqualTo("KRX");
    }
}
