package com.graphify.market;

import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * KOSPI200 일봉 적재를 백그라운드(fire-and-forget)로 실행한다.
 * - 관리자 트리거는 즉시 반환(202)하고 실제 적재는 단일 워커 스레드에서 진행 → HTTP 타임아웃/ CORS 마스킹 회피.
 * - 동시 실행은 running 플래그로 1건만 허용(중복 클릭 방지).
 * - 마지막 실행 상태(state/symbols/error)를 메모리에 보관해 상태 조회 엔드포인트로 노출.
 */
@Component
public class MarketIngestionJobRunner {

    private static final Logger log = LoggerFactory.getLogger(MarketIngestionJobRunner.class);

    private final MarketDataIngestionService ingestionService;
    private final Executor ingestExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile JobStatus lastStatus = JobStatus.idle();

    public MarketIngestionJobRunner(
            MarketDataIngestionService ingestionService,
            @Qualifier("ingestExecutor") Executor ingestExecutor) {
        this.ingestionService = ingestionService;
        this.ingestExecutor = ingestExecutor;
    }

    public enum State { IDLE, RUNNING, DONE, FAILED }

    /** 마지막(또는 진행 중) 적재 작업 상태. 시각은 ISO-8601 문자열. */
    public record JobStatus(
            State state,
            Integer symbols,
            String startedAt,
            String finishedAt,
            String error) {

        static JobStatus idle() {
            return new JobStatus(State.IDLE, null, null, null, null);
        }
    }

    public JobStatus status() {
        return lastStatus;
    }

    /**
     * 적재 작업을 시작한다. 이미 실행 중이면 false 반환(트리거 무시).
     * 실제 적재는 워커 스레드에서 수행되며, 본 메서드는 즉시 반환한다.
     */
    public boolean tryStartKospi200DailyIngest() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        String startedAt = Instant.now().toString();
        lastStatus = new JobStatus(State.RUNNING, null, startedAt, null, null);
        ingestExecutor.execute(() -> {
            try {
                int symbols = ingestionService.ingestDailyForKospi200();
                lastStatus = new JobStatus(State.DONE, symbols, startedAt, Instant.now().toString(), null);
                log.info("KOSPI200 async ingest done: {} symbols", symbols);
            } catch (Exception e) {
                lastStatus = new JobStatus(State.FAILED, null, startedAt, Instant.now().toString(), e.toString());
                log.error("KOSPI200 async ingest failed", e);
            } finally {
                running.set(false);
            }
        });
        return true;
    }
}
