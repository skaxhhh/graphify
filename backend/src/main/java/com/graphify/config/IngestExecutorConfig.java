package com.graphify.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 관리자 트리거 일봉 적재(ingest-kospi200)를 백그라운드로 돌리기 위한 단일 워커 풀.
 * 동시 실행은 1건으로 제한(중복 트리거는 {@link com.graphify.market.MarketIngestionJobRunner}의
 * running 플래그로 차단). HTTP 요청 스레드를 점유하지 않으려는 목적이다.
 *
 * 주의: Cloud Run은 기본적으로 "요청 처리 중에만 CPU 할당"이라, 202 응답 후 이 워커가
 * 진행하려면 인스턴스 CPU가 계속 살아 있어야 한다(= "CPU always allocated" 또는 min-instances>=1).
 */
@Configuration
public class IngestExecutorConfig {

    @Bean(name = "ingestExecutor")
    public Executor ingestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("ingest-");
        executor.initialize();
        return executor;
    }
}
