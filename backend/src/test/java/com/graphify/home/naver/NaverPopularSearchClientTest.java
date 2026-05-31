package com.graphify.home.naver;

import static org.assertj.core.api.Assertions.assertThat;

import com.graphify.config.GraphifyMarketProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NaverPopularSearchClientTest {

    @Test
    void parsePopularList_extractsRankTickerAndName() {
        GraphifyMarketProperties props = new GraphifyMarketProperties();
        props.setNaverEnabled(true);
        NaverPopularSearchClient client = new NaverPopularSearchClient(
                props,
                RestClient.builder().baseUrl("https://finance.naver.com").build()
        );

        String html = """
                <ul class="lst_pop" id="popularItemList">
                <li><em>1.</em><a href="/item/main.naver?code=005930">삼성전자</a><span class="up">299,000</span></li>
                <li><em>2.</em><a href="/item/main.naver?code=000660">SK하이닉스</a><span class="down">2,052,000</span></li>
                </ul>
                """;

        var items = client.parsePopularList(html);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).ticker()).isEqualTo("005930");
        assertThat(items.get(0).name()).isEqualTo("삼성전자");
        assertThat(items.get(0).price()).isEqualTo(299_000.0);
    }
}
