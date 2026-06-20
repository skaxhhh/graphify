package com.graphify.trading.rule.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DATA-03: Universe JSON 역직렬화 검증.
 * volume_top_n 신규 필드(market, topN, additionalSymbols)가 올바르게 매핑된다.
 */
class RuleDefinitionUniverseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // Test 1: volume_top_n 전체 필드 역직렬화
    @Test
    void volumeTopN_withAllFields_deserializesCorrectly() throws Exception {
        String json = """
                {
                  "type": "volume_top_n",
                  "market": "KOSPI",
                  "topN": 10,
                  "additionalSymbols": ["005930"]
                }
                """;

        RuleDefinition.Universe universe = mapper.readValue(json, RuleDefinition.Universe.class);

        assertThat(universe.type()).isEqualTo("volume_top_n");
        assertThat(universe.market()).isEqualTo("KOSPI");
        assertThat(universe.topN()).isEqualTo(10);
        assertThat(universe.additionalSymbols()).containsExactly("005930");
    }

    // Test 2: 기존 symbols 타입이 하위 호환 유지
    @Test
    void symbols_existingType_stillDeserializesCorrectly() throws Exception {
        String json = """
                {
                  "type": "symbols",
                  "symbols": ["005930"]
                }
                """;

        RuleDefinition.Universe universe = mapper.readValue(json, RuleDefinition.Universe.class);

        assertThat(universe.type()).isEqualTo("symbols");
        assertThat(universe.symbols()).containsExactly("005930");
    }

    // Test 3: volume_top_n에 symbols 필드 없어도 역직렬화 오류 없음
    @Test
    void volumeTopN_withoutSymbolsField_deserializesWithoutError() throws Exception {
        String json = """
                {
                  "type": "volume_top_n",
                  "market": "KOSPI",
                  "topN": 10
                }
                """;

        RuleDefinition.Universe universe = mapper.readValue(json, RuleDefinition.Universe.class);

        assertThat(universe.type()).isEqualTo("volume_top_n");
        assertThat(universe.symbols()).isNull();
    }

    // Test 4: additionalSymbols 없는 volume_top_n → additionalSymbols()=null
    @Test
    void volumeTopN_withoutAdditionalSymbols_returnsNull() throws Exception {
        String json = """
                {
                  "type": "volume_top_n",
                  "market": "KOSPI",
                  "topN": 5
                }
                """;

        RuleDefinition.Universe universe = mapper.readValue(json, RuleDefinition.Universe.class);

        assertThat(universe.topN()).isEqualTo(5);
        assertThat(universe.additionalSymbols()).isNull();
    }
}
