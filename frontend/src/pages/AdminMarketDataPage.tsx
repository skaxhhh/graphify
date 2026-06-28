import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { ApiRequestError } from "@/lib/apiClient";
import { ingestKospi200, seedKospi200, type Kospi200Counts } from "@/lib/adminApi";

const COUNT_LABELS: Record<string, string> = {
  inserted: "추가",
  updated: "갱신",
  flagged: "플래그",
  ingested: "적재",
  count: "건수",
  symbols: "종목",
  total: "합계",
  failed: "실패",
};

function formatCounts(counts: Kospi200Counts | null | undefined): string {
  if (!counts) return "완료";
  const parts = Object.entries(counts).map(
    ([key, value]) => `${COUNT_LABELS[key] ?? key} ${value}`
  );
  return parts.length > 0 ? parts.join(" · ") : "완료";
}

export function AdminMarketDataPage() {
  const [toast, setToast] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!toast) return;
    const id = window.setTimeout(() => setToast(null), 5000);
    return () => window.clearTimeout(id);
  }, [toast]);

  const seedMutation = useMutation({
    mutationFn: () => seedKospi200(),
    onSuccess: (res) => {
      setError(null);
      setToast(`KOSPI200 시드 완료 — ${formatCounts(res.data)}`);
    },
    onError: (err) => {
      setToast(null);
      setError(
        err instanceof ApiRequestError ? err.message : "시드 실행에 실패했습니다."
      );
    },
  });

  const ingestMutation = useMutation({
    mutationFn: () => ingestKospi200(),
    onSuccess: (res) => {
      setError(null);
      setToast(`KOSPI200 일봉 적재 완료 — ${formatCounts(res.data)}`);
    },
    onError: (err) => {
      setToast(null);
      setError(
        err instanceof ApiRequestError ? err.message : "적재 실행에 실패했습니다."
      );
    },
  });

  const anyPending = seedMutation.isPending || ingestMutation.isPending;

  return (
    <div className="mx-auto w-full max-w-[800px] space-y-8 p-6 md:p-8">
      <header>
        <h1 className="text-2xl font-semibold text-charcoal">시장 데이터</h1>
        <p className="mt-1 text-sm text-muted-gray">
          KOSPI200 종목 마스터 시드와 일봉 적재를 실행합니다. 두 작업 모두 멱등하게 재실행할 수
          있습니다.
        </p>
      </header>

      {toast ? (
        <p
          className="rounded-lg border border-warm-border bg-cream px-4 py-2 text-sm text-charcoal"
          role="status"
        >
          {toast}
        </p>
      ) : null}

      {error ? (
        <p
          className="rounded-lg border border-warm-border bg-cream px-4 py-2 text-sm text-charcoal"
          role="alert"
        >
          {error}
        </p>
      ) : null}

      <section className="space-y-4 rounded-xl border border-warm-border bg-cream p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-base font-medium text-charcoal">KOSPI200 마스터 시드</h2>
            <p className="mt-1 text-sm text-muted-gray">
              큐레이션 리스트(kospi200.csv)를 종목 마스터에 UPSERT하고 in_kospi200 플래그를
              설정합니다.
            </p>
          </div>
          <PrimaryButton
            className="w-auto px-6"
            loading={seedMutation.isPending}
            disabled={anyPending}
            onClick={() => seedMutation.mutate()}
          >
            시드 실행
          </PrimaryButton>
        </div>

        <div className="border-t border-warm-border" />

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-base font-medium text-charcoal">KOSPI200 일봉 적재</h2>
            <p className="mt-1 text-sm text-muted-gray">
              KOSPI200 구성종목의 일봉 데이터를 적재합니다. 시드 후 실행하세요.
            </p>
          </div>
          <PrimaryButton
            className="w-auto px-6"
            loading={ingestMutation.isPending}
            disabled={anyPending}
            onClick={() => ingestMutation.mutate()}
          >
            적재 실행
          </PrimaryButton>
        </div>
      </section>
    </div>
  );
}
