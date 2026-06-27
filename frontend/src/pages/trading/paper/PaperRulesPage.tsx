// PaperRulesPage — 전략 설정: config축(DRAFT/ACTIVE)만 제어, run 제어 미노출 (06.5-05)
// 06.8-02: Reskinned to Binance dark tokens; all CRUD/2-axis/GUARD behavior unchanged.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { deletePaperRule, fetchPaperRules } from "@/lib/ruleApi";
import { activateRule, copyRule, deactivateRule } from "@/lib/paperApi";
import type { TradingRule } from "@/types/trading";
import {
  TradeBadge,
  TradeButton,
  TradePageState,
  TradeTable,
  TradeTableHeader,
  TradeTableRow,
} from "@/components/trading/ui";

const COL = "grid grid-cols-[2fr_1fr_1fr_1fr_1.4fr] items-center";

export function PaperRulesPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { data: rules, isLoading, isError } = useQuery({
    queryKey: ["trading", "paper", "rules"],
    queryFn: async () => (await fetchPaperRules()).data ?? [],
  });

  const invalidate = () =>
    void queryClient.invalidateQueries({ queryKey: ["trading", "paper", "rules"] });

  const activateMutation = useMutation({
    mutationFn: (id: number) => activateRule(id),
    onSuccess: invalidate,
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => deactivateRule(id),
    onSuccess: invalidate,
  });

  const copyMutation = useMutation({
    mutationFn: (id: number) => copyRule(id),
    onSuccess: invalidate,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deletePaperRule(id),
    onSuccess: invalidate,
  });

  const anyPending =
    activateMutation.isPending ||
    deactivateMutation.isPending ||
    copyMutation.isPending ||
    deleteMutation.isPending;

  const mutationError =
    activateMutation.error ??
    deactivateMutation.error ??
    copyMutation.error ??
    deleteMutation.error;

  return (
    <div>
      {/* Header — wireframe lines ~323–326 */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-trade-on-dark">전략 설정</h2>
          <p className="mt-1 text-sm text-trade-muted">
            룰 CRUD · 설정 축(DRAFT ↔ ACTIVE)
          </p>
        </div>
        <TradeButton
          variant="primary"
          onClick={() => navigate("/trading/paper/rules/new")}
        >
          + 새 룰
        </TradeButton>
      </div>

      {/* Failure banner — bg-trade-down-soft border-trade-down/30 (wireframe lines ~327–329) */}
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
      ) : (rules ?? []).length === 0 ? (
        <TradePageState
          variant="empty"
          title="등록된 룰이 없습니다"
          message='+ 새 룰로 추가하세요.'
        />
      ) : (
        <TradeTable>
          {/* Header — wireframe lines ~331–333 */}
          <TradeTableHeader className={COL}>
            <span>룰 이름</span>
            <span>설정 상태</span>
            <span>쿨다운</span>
            <span>수정일</span>
            <span className="text-right">관리</span>
          </TradeTableHeader>

          {/* Rows */}
          {(rules ?? []).map((rule: TradingRule) => {
            const configStatus = rule.configStatus ?? "DRAFT";
            const runStatus = rule.runStatus ?? "STOPPED";
            const isRunning = runStatus === "RUNNING";
            const isActive = configStatus === "ACTIVE";

            return (
              <TradeTableRow key={rule.id} className={COL}>
                {/* 이름 — running indicator inline (wireframe line ~339) */}
                <span className="text-sm text-trade-body">
                  {rule.name}
                  {isRunning ? (
                    <span className="ml-2 font-trade-mono text-[10px] text-trade-up">
                      ●실행중
                    </span>
                  ) : null}
                </span>

                {/* 설정 상태 — TradeBadge active/draft */}
                <span>
                  <TradeBadge variant={isActive ? "active" : "draft"}>
                    {isActive ? "ACTIVE" : "DRAFT"}
                  </TradeBadge>
                </span>

                {/* 쿨다운 — font-trade-mono */}
                <span className="font-trade-mono text-sm text-trade-muted-strong">
                  {rule.definition.constraints?.cooldownBars != null
                    ? `${rule.definition.constraints.cooldownBars}봉·${rule.definition.constraints.cooldownBars * 5}분`
                    : "—"}
                </span>

                {/* 수정일 — font-trade-mono */}
                <span className="font-trade-mono text-sm text-trade-muted">
                  {new Date(rule.updatedAt).toLocaleDateString("ko-KR", {
                    month: "2-digit",
                    day: "2-digit",
                  })}
                </span>

                {/* 관리 — action links */}
                <span className="flex items-center justify-end gap-2.5 text-xs">
                  {/* ACTIVE 토글 — run 제어 없음 */}
                  {isActive ? (
                    <button
                      type="button"
                      onClick={() => deactivateMutation.mutate(rule.id)}
                      disabled={anyPending || isRunning}
                      title={
                        isRunning
                          ? "전략 운영에서 먼저 중지"
                          : "DRAFT로 하향"
                      }
                      className="text-trade-primary hover:underline disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      하향
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => activateMutation.mutate(rule.id)}
                      disabled={anyPending}
                      title="ACTIVE로 전환"
                      className="text-trade-up hover:underline disabled:opacity-40"
                    >
                      활성화
                    </button>
                  )}

                  <span className="text-trade-hairline">·</span>

                  {/* 편집 — ACTIVE+RUNNING 시 비활성 (GUARD) */}
                  <button
                    type="button"
                    onClick={() => navigate(`/trading/paper/rules/edit/${rule.id}`)}
                    disabled={isActive && isRunning}
                    title={
                      isActive && isRunning
                        ? "전략 운영에서 먼저 중지"
                        : "편집"
                    }
                    className="text-trade-muted-strong hover:underline disabled:cursor-not-allowed disabled:opacity-30"
                  >
                    편집
                  </button>

                  <span className="text-trade-hairline">·</span>

                  <button
                    type="button"
                    onClick={() => copyMutation.mutate(rule.id)}
                    disabled={anyPending}
                    className="text-trade-muted-strong hover:underline disabled:opacity-40"
                  >
                    복제
                  </button>

                  <span className="text-trade-hairline">·</span>

                  <button
                    type="button"
                    onClick={() => deleteMutation.mutate(rule.id)}
                    disabled={anyPending}
                    className="text-trade-down hover:underline disabled:opacity-40"
                  >
                    삭제
                  </button>
                </span>
              </TradeTableRow>
            );
          })}
        </TradeTable>
      )}
    </div>
  );
}
