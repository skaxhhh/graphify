package com.graphify.trading.rule;

/**
 * 룰 상태 술어 헬퍼. 라이브 루프(인제스트/평가)에 포함되어야 하는 상태를 단일 지점에서 정의한다.
 * Phase 6.5: 2축 상태 모델 도입 — run_status=RUNNING 기준으로 전환.
 * Phase 8 LIVE 승격 시: isLiveActive(TradingRule)에 LIVE 모드 조건 추가만으로 인제스트+평가 양쪽 확장 가능 (single edit point).
 */
public final class RuleStatus {

    public static final String PAPER_LIVE = "PAPER_LIVE";
    public static final String RUNNING = "RUNNING";
    public static final String ACTIVE = "ACTIVE";

    private RuleStatus() {}

    /**
     * @deprecated Phase 6.5 이후 사용하지 않음. isLiveActive(TradingRule) 사용 권장.
     * 하위 호환성을 위해 보존.
     */
    @Deprecated
    public static boolean isLiveActive(String status) {
        return PAPER_LIVE.equals(status);
    }

    /**
     * 라이브 루프(스케줄러 인제스트 + LiveEvaluationService 평가)에 포함되는 상태인지.
     * 2축 상태 기준: run_status = 'RUNNING' 체크.
     * Phase 8: || "LIVE".equals(rule.getMode())  ← LIVE 승격 시 이 한 줄만 확장.
     */
    public static boolean isLiveActive(TradingRule rule) {
        return RUNNING.equals(rule.getRunStatus());
    }
}
