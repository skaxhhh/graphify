// TradingRulesPage — 전략 운영: ACTIVE 룰만 표시, run축(STOPPED/RUNNING)만 제어 (06.5-05)
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchPaperRules } from "@/lib/ruleApi";
import { copyRule, startRule, stopRule } from "@/lib/paperApi";
import { ApiRequestError } from "@/lib/apiClient";
import { CompanyPickerModal } from "@/components/shared/CompanyPickerModal";
import type { TradingRule } from "@/types/trading";

// ---- run 상태 배지 ----
function RunBadge({ runStatus }: { runStatus: "STOPPED" | "RUNNING" }) {
  if (runStatus === "RUNNING") {
    return (
      <span className="rounded bg-emerald-700/60 px-2 py-0.5 text-xs font-medium text-emerald-300">
        실행 중
      </span>
    );
  }
  return (
    <span className="rounded bg-yellow-700/40 px-2 py-0.5 text-xs font-medium text-yellow-400">
      중지됨
    </span>
  );
}

export function TradingRulesPage() {
  const queryClient = useQueryClient();
  const invalidate = () =>
    void queryClient.invalidateQueries({ queryKey: ["trading", "paper", "rules"] });

  const { data: allRules, isLoading, isError } = useQuery({
    queryKey: ["trading", "paper", "rules"],
    queryFn: async () => (await fetchPaperRules()).data ?? [],
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
      <div className="mb-6">
        <h2 className="text-xl font-semibold text-white">전략 운영</h2>
        <p className="mt-1 text-sm text-gray-400">
          ACTIVE 전략의 실행 상태를 관리합니다. 설정 변경은 "전략 설정" 화면에서 합니다.
        </p>
      </div>

      {mutationError ? (
        <div className="mb-4 rounded-md bg-red-900/40 px-4 py-2 text-sm text-red-300">
          {mutationError instanceof Error ? mutationError.message : "작업에 실패했습니다."}
        </div>
      ) : null}

      {isLoading ? (
        <p className="text-sm text-gray-400">불러오는 중...</p>
      ) : isError ? (
        <p className="text-sm text-red-400">룰 목록을 불러오지 못했습니다.</p>
      ) : rules.length === 0 ? (
        <p className="rounded-lg border border-white/10 bg-gray-900/50 p-6 text-sm text-gray-400">
          ACTIVE 상태인 룰이 없습니다. 전략 설정 화면에서 룰을 활성화하세요.
        </p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-white/10 bg-gray-900/50">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-gray-400">
                <th className="px-4 py-3 font-medium">이름</th>
                <th className="px-4 py-3 font-medium">실행 상태</th>
                <th className="px-4 py-3 font-medium">수정일</th>
                <th className="px-4 py-3 text-right font-medium">제어</th>
              </tr>
            </thead>
            <tbody>
              {rules.map((rule: TradingRule) => {
                const runStatus = rule.runStatus ?? "STOPPED";
                const isRunning = runStatus === "RUNNING";

                return (
                  <tr key={rule.id} className="border-b border-white/5 last:border-0">
                    <td className="px-4 py-3 font-medium text-white">{rule.name}</td>
                    <td className="px-4 py-3">
                      <RunBadge runStatus={runStatus} />
                    </td>
                    <td className="px-4 py-3 text-gray-400">
                      {new Date(rule.updatedAt).toLocaleDateString("ko-KR")}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        {isRunning ? (
                          <button
                            type="button"
                            disabled={anyPending}
                            onClick={() => stopMutation.mutate(rule.id)}
                            className="rounded bg-yellow-600 px-2 py-1 text-xs text-white hover:opacity-90 disabled:opacity-40"
                          >
                            중지
                          </button>
                        ) : (
                          <button
                            type="button"
                            disabled={anyPending}
                            onClick={() => startMutation.mutate({ id: rule.id })}
                            className="rounded bg-emerald-600 px-2 py-1 text-xs text-white hover:opacity-90 disabled:opacity-40"
                          >
                            시작
                          </button>
                        )}
                        <button
                          type="button"
                          disabled={anyPending}
                          onClick={() => copyMutation.mutate(rule.id)}
                          className="rounded border border-white/20 px-2 py-1 text-xs text-gray-300 hover:bg-white/5 disabled:opacity-40"
                        >
                          복사
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <CompanyPickerModal
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
