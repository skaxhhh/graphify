package com.graphify.trading.rule;

/**
 * 룰 상태 술어 헬퍼. 라이브 루프(인제스트/평가)에 포함되어야 하는 상태를 단일 지점에서 정의한다.
 * Phase 6: LIVE 승격이 추가되면 isLiveActive()에 "LIVE"를 추가하는 것만으로 인제스트+평가 양쪽이 함께 확장된다 (single edit point).
 */
public final class RuleStatus {

    public static final String PAPER_LIVE = "PAPER_LIVE";

    private RuleStatus() {}

    /**
     * 라이브 루프(스케줄러 인제스트 + LiveEvaluationService 평가)에 포함되는 상태인지.
     * Phase 6: || "LIVE".equals(status)  ← LIVE 승격 시 이 한 줄만 확장.
     */
    public static boolean isLiveActive(String status) {
        return PAPER_LIVE.equals(status);
    }
}
