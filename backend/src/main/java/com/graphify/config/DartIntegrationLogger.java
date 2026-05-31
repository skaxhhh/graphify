package com.graphify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DartIntegrationLogger {

    private static final Logger log = LoggerFactory.getLogger(DartIntegrationLogger.class);

    private final GraphifyDartProperties dartProperties;

    public DartIntegrationLogger(GraphifyDartProperties dartProperties) {
        this.dartProperties = dartProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDartStatus() {
        if (dartProperties.hasApiKey()) {
            log.info("Open DART 연동: API 키 설정됨 (enrich 검색 사용 가능)");
        } else {
            log.warn("Open DART 연동: API 키 없음 — DART_API_KEY 를 루트 .env 에 설정하세요");
        }
    }
}
