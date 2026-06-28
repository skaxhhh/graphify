// TradingRulesPage — 전략 운영: ACTIVE 룰만 표시, run축(STOPPED/RUNNING)만 제어 (06.5-05)
// 06.8-02: Reskinned to Binance dark tokens; ACTIVE filter + start/stop handlers unchanged.
// Shared by PAPER (/paper/rules-lifecycle) and LIVE (/rules) routes.
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchPaperRules } from "@/lib/ruleApi";
import { copyRule, fetchPaperMonitor, startRule, stopRule } from "@/lib/paperApi";
import { ApiRequestError } from "@/lib/apiClient";
import { TradingCompanyPickerModal } from "@/components/trading/TradingCompanyPickerModal";
import type { TradingRule } from "@/types/trading";
import {
  TradeBadge,
  TradeButton,
  TradeCard,
  TradePageState,
  TradeStatCard,
  TradeTable,
  TradeTableHeader,
  TradeTableRow,
} from "@/components/trading/ui";

const COL = "grid grid-cols-[2.2fr_1.2fr_1fr_1.6fr] items-center";

export function TradingRulesPage() {
  const queryClient = useQueryClient();
  const invalidate = () =>
    void queryClient.invalidateQueries({ queryKey: ["trading", "paper", "rules"] });

  const { data: allRules, isLoading, isError } = useQuery({
    queryKey: ["trading", "paper", "rules"],
    queryFn: async () => (await fetchPaperRules()).data ?? [],
  });

  // D3: 동작 모니터링 삭제 → 시장 상태·스케줄러만 상단 위젯으로 이동
  // 신호로그·오늘 거래는 렌더하지 않음
  const { data: monitorData } = useQuery({
    queryKey: ["trading", "paper", "monitor"],
    queryFn: async () => (await fetchPaperMonitor()).data ?? null,
    // graceful: 실패 시 undefined → "—" 표시
  });

  // 전략 운영 화면: ACTIVE 룰만 표시 (DRAFT는 전략 설정에서만 보임)
  const rules = (allRules ?? []).filter(
    (r: TradingRule) => (r.configStatus ?? "DRAFT") === "ACTIVE"
  );

  // v1.6.0: 빈 실시간 유니버스(ERR_LIFECYCLE_005) 폴백 — 종목 직접 선택 모달
  const [pickerRuleId, setPickerRuleId] = useState<number | null>(null);

  const startMutation = useMutation({
    mutationFn: ({ id, overrideSymbols }: { id: number; overrideSymbols?: string[] }) =>
      startRule(id, overrideSymbols),
    onSuccess: () => {
      setPickerRuleId(null);
      invalidate();
    },
    onError: (err, variables) => {
      // 실시간 거래대금 랭킹 조회 실패 → 종목 직접 선택으로 폴백
      if (
        err instanceof ApiRequestError &&
        err.code === "ERR_LIFECYCLE_005" &&
        !variables.overrideSymbols
      ) {
        setPickerRuleId(variables.id);
      }
    },
  });
  const stopMutation  = useMutation({ mutationFn: stopRule,  onSuccess: invalidate });
  const copyMutation  = useMutation({ mutationFn: copyRule,  onSuccess: invalidate });

  const anyPending =
    startMutation.isPending || stopMutation.isPending || copyMutation.isPending;

  const mutationError =
    startMutation.error ?? stopMutation.error ?? copyMutation.error;

  return (
    <div>
      {/* Header — wireframe lines ~358–360 */}
      <div className="mb-6">
        <h2 className="text-xl font-semibold text-trade-on-dark">전략 운영</h2>
        <p className="mt-1 text-sm text-trade-muted">
          ACTIVE 룰만 표시 · 실행 축(STOPPED ↔ RUNNING)
        </p>
      </div>

      {/* D3 위젯: 시장 상태 + 스케줄러 최근 실행 (신호로그·오늘 거래 미노출) */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* 시장 상태 카드 */}
        <TradeCard title="시장 상태">
          <div className="flex items-center gap-2">
            <span
              className={`h-2 w-2 shrink-0 rounded-full ${
                monitorData?.marketStatus === "OPEN"
                  ? "bg-trade-up"
                  : "bg-trade-muted"
              }`}
            />
            <span className="text-sm font-semibold text-trade-body font-trade-sans">
              {monitorData?.marketStatus === "OPEN" ? "개장" : "폐장"}
            </span>
          </div>
        </TradeCard>

        {/* 스케줄러 최근 실행 카드 */}
        <TradeStatCard
          label="스케줄러 최근 실행"
          value={
            monitorData?.schedulerLastRun
              ? new Date(monitorData.schedulerLastRun).toLocaleString("ko-KR", {
                  month: "2-digit",
                  day: "2-digit",
                  hour: "2-digit",
                  minute: "2-digit",
                })
              : "—"
          }
        />
      </div>

      {/* Mutation error banner */}
      {mutationError ? (
        <div className="mb-4 rounded-lg border border-trade-down/30 bg-trade-down-soft px-4 py-2.5 text-sm text-trade-down">
          {mutationError instanceof Error ? mutationError.message : "작업에 실패했습니다."}
        </div>
      ) : null}

      {/* Loading / Error / Empty / Table */}
      {isLoading ? (
        <TradePageState variant="loading" />
      ) : isError ? (
        <TradePageState
          variant="error"
          title="룰 목록 로드 실패"
          message="룰 목록을 불러오지 못했습니다."
        />
      ) : rules.length === 0 ? (
        <TradePageState
          variant="empty"
          title="ACTIVE 룰 없음"
          message="ACTIVE 상태인 룰이 없습니다. 전략 설정에서 활성화하세요."
        />
      ) : (
        <TradeTable>
          {/* Header — wireframe lines ~361–363 */}
          <TradeTableHeader className={COL}>
            <span>룰 이름</span>
            <span>실행 상태</span>
            <span>수정일</span>
            <span className="text-right">제어</span>
          </TradeTableHeader>

          {/* Rows */}
          {rules.map((rule: TradingRule) => {
            const runStatus = rule.runStatus ?? "STOPPED";
            const isRunning = runStatus === "RUNNING";

            return (
              <TradeTableRow key={rule.id} className={COL}>
                {/* 이름 */}
                <span className="text-sm font-medium text-trade-body">{rule.name}</span>

                {/* 실행 상태 — TradeBadge running/stopped (wireframe lines ~365/369) */}
                <span>
                  <TradeBadge variant={isRunning ? "running" : "stopped"}>
                    {isRunning ? "● 실행 중" : "중지됨"}
                  </TradeBadge>
                </span>

                {/* 수정일 — font-trade-mono */}
                <span className="font-trade-mono text-sm text-trade-muted">
                  {new Date(rule.updatedAt).toLocaleDateString("ko-KR", {
                    month: "2-digit",
                    day: "2-digit",
                  })}
                </span>

                {/* 제어 — start/stop + 복사 */}
                <div className="flex flex-wrap items-center justify-end gap-2">
                  {isRunning ? (
                    /* 중지 → TradeButton danger (wireframe line ~366: bg red) */
                    <TradeButton
                      variant="danger"
                      size="sm"
                      disabled={anyPending}
                      onClick={() => stopMutation.mutate(rule.id)}
                    >
                      중지
                    </TradeButton>
                  ) : (
                    /* 시작 → green button (bg-trade-up, text-trade-on-dark; wireframe line ~370) */
                    <button
                      type="button"
                      disabled={anyPending}
                      onClick={() => startMutation.mutate({ id: rule.id })}
                      className="inline-flex h-7 items-center justify-center rounded bg-trade-up px-3 text-xs font-semibold text-trade-on-dark hover:opacity-90 disabled:opacity-40"
                    >
                      시작
                    </button>
                  )}

                  {/* 복사 — ghost */}
                  <TradeButton
                    variant="ghost"
                    size="sm"
                    disabled={anyPending}
                    onClick={() => copyMutation.mutate(rule.id)}
                  >
                    복사
                  </TradeButton>
                </div>
              </TradeTableRow>
            );
          })}
        </TradeTable>
      )}

      {/* Trade-themed stock picker (폴백 모달) — swapped from shared/CompanyPickerModal */}
      <TradingCompanyPickerModal
        open={pickerRuleId !== null}
        title="실시간 종목 직접 선택"
        description="실시간 거래대금 순위를 가져오지 못했습니다. 거래할 종목을 직접 선택하세요."
        confirmLabel="이 종목으로 시작"
        onClose={() => setPickerRuleId(null)}
        onConfirm={(symbols) => {
          if (pickerRuleId !== null) {
            startMutation.mutate({ id: pickerRuleId, overrideSymbols: symbols });
          }
        }}
      />
    </div>
  );
}
