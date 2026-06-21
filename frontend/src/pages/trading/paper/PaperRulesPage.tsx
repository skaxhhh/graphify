import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { deletePaperRule, fetchPaperRules } from "@/lib/ruleApi";
import { copyRule } from "@/lib/paperApi";
import type { TradingRule } from "@/types/trading";

export function PaperRulesPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { data: rules, isLoading, isError } = useQuery({
    queryKey: ["trading", "paper", "rules"],
    queryFn: async () => (await fetchPaperRules()).data ?? [],
  });

  const invalidate = () =>
    void queryClient.invalidateQueries({ queryKey: ["trading", "paper", "rules"] });

  const copyMutation = useMutation({
    mutationFn: (id: number) => copyRule(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["trading", "paper", "rules"] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deletePaperRule(id),
    onSuccess: invalidate,
  });

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-white">모의 룰 설정</h2>
          <p className="mt-1 text-sm text-gray-400">
            모의투자에 사용할 매매 룰을 정의합니다. 변경은 다음 평가 주기부터 반영됩니다.
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate("/trading/paper/rules/new")}
          className="rounded-md bg-emerald-600 px-4 py-2 text-sm text-white transition-opacity hover:opacity-90"
        >
          + 새 룰
        </button>
      </div>

      {isLoading ? (
        <p className="text-sm text-gray-400">불러오는 중...</p>
      ) : isError ? (
        <p className="text-sm text-red-400">룰 목록을 불러오지 못했습니다.</p>
      ) : (rules ?? []).length === 0 ? (
        <p className="rounded-lg border border-white/10 bg-gray-900/50 p-6 text-sm text-gray-400">
          등록된 룰이 없습니다. "+ 새 룰"로 추가하세요.
        </p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-white/10 bg-gray-900/50">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-gray-400">
                <th className="px-4 py-3 font-medium">이름</th>
                <th className="px-4 py-3 font-medium">상태</th>
                <th className="px-4 py-3 font-medium">쿨다운</th>
                <th className="px-4 py-3 font-medium">수정일</th>
                <th className="px-4 py-3 text-right font-medium">관리</th>
              </tr>
            </thead>
            <tbody>
              {(rules ?? []).map((rule: TradingRule) => (
                <tr key={rule.id} className="border-b border-white/5 last:border-0">
                  <td className="px-4 py-3 text-white">{rule.name}</td>
                  <td className="px-4 py-3">
                    <span className="rounded bg-white/10 px-2 py-0.5 text-xs text-gray-300">
                      {rule.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-400">
                    {rule.definition.constraints?.cooldownBars != null
                      ? `${rule.definition.constraints.cooldownBars}봉 (${rule.definition.constraints.cooldownBars * 5}m)`
                      : "—"}
                  </td>
                  <td className="px-4 py-3 text-gray-400">
                    {new Date(rule.updatedAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      type="button"
                      onClick={() => navigate(`/trading/paper/rules/edit/${rule.id}`)}
                      className="mr-2 text-emerald-400 hover:underline"
                    >
                      편집
                    </button>
                    <button
                      type="button"
                      onClick={() => copyMutation.mutate(rule.id)}
                      disabled={copyMutation.isPending}
                      className="mr-2 text-blue-400 hover:underline disabled:opacity-50"
                    >
                      복제
                    </button>
                    <button
                      type="button"
                      onClick={() => deleteMutation.mutate(rule.id)}
                      disabled={deleteMutation.isPending}
                      className="text-red-400 hover:underline disabled:opacity-50"
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
