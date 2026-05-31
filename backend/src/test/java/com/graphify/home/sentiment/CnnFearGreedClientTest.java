package com.graphify.home.sentiment;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.config.GraphifyMarketProperties;
import com.graphify.home.dto.MarketSentimentSnapshotDto;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CnnFearGreedClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void parseSnapshot_mapsCnnScoreAndVix() throws Exception {
    GraphifyMarketProperties props = new GraphifyMarketProperties();
    CnnFearGreedClient client = new CnnFearGreedClient(props, null);

    String json =
        new ClassPathResource("cnn-fear-greed-sample.json")
            .getContentAsString(StandardCharsets.UTF_8);
    MarketSentimentSnapshotDto snapshot =
        client.parseSnapshot(objectMapper.readTree(json));

    assertThat(snapshot.score()).isBetween(58.0, 60.0);
    assertThat(snapshot.zone()).isEqualTo("GREED");
    assertThat(snapshot.dataSource()).isEqualTo("CNN_OFFICIAL");
    assertThat(snapshot.market()).isEqualTo("US (CNN)");
    assertThat(snapshot.indicators()).hasSize(7);
    assertThat(snapshot.vix()).isNotNull();
    assertThat(snapshot.vixMa50()).isNotNull();
  }
}
