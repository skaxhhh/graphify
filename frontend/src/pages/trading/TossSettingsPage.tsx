import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  fetchTossStatus,
  refreshTossToken,
  saveTossCredentials,
  type TossCredentialStatus,
} from "@/lib/tossApi";
import { TradeBadge, TradeButton, TradeCard, TradeInput } from "@/components/trading/ui";

function fmtKst(iso: string | null): string {
  if (!iso) return "-";
  return new Date(iso).toLocaleString("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function statusDotColor(status: TossCredentialStatus | undefined): string {
  if (!status || !status.configured) return "bg-trade-muted";
  if (status.tokenValid) return "bg-trade-up";
  return "bg-trade-down";
}

function StatusBadge({ status }: { status: TossCredentialStatus | undefined }) {
  if (!status) return null;
  if (!status.configured) {
    return <TradeBadge variant="draft">미설정</TradeBadge>;
  }
  if (status.tokenValid) {
    return <TradeBadge variant="up">설정됨 · 토큰 유효</TradeBadge>;
  }
  return <TradeBadge variant="down">설정됨 · 토큰 만료</TradeBadge>;
}

export function TossSettingsPage() {
  const queryClient = useQueryClient();
  const invalidate = () =>
    void queryClient.invalidateQueries({ queryKey: ["toss", "status"] });

  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(
    null
  );

  const { data: status, isLoading } = useQuery({
    queryKey: ["toss", "status"],
    queryFn: async () => (await fetchTossStatus()).data ?? undefined,
  });

  const saveMutation = useMutation({
    mutationFn: () => saveTossCredentials(clientId, clientSecret),
    onSuccess: () => {
      setMessage({ type: "success", text: "자격증명이 저장되었습니다." });
      setClientId("");
      setClientSecret("");
      invalidate();
    },
    onError: (err: Error) => {
      setMessage({ type: "error", text: err.message ?? "저장에 실패했습니다." });
    },
  });

  const refreshMutation = useMutation({
    mutationFn: refreshTossToken,
    onSuccess: () => {
      setMessage({ type: "success", text: "토큰이 갱신되었습니다." });
      invalidate();
    },
    onError: (err: Error) => {
      setMessage({ type: "error", text: err.message ?? "토큰 갱신에 실패했습니다." });
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    saveMutation.mutate();
  };

  return (
    <div className="max-w-lg space-y-6">
      <div>
        <h2 className="font-trade-sans text-xl font-semibold text-trade-on-dark">토스 설정</h2>
        <p className="mt-1 font-trade-sans text-sm text-trade-muted">
          토스증권 Open API 자격증명을 등록합니다. 입력 정보는 AES-256-GCM으로 암호화되어
          저장됩니다.
        </p>
      </div>

      {/* Status card */}
      <TradeCard>
        <div className="flex items-center justify-between">
          <div>
            <p className="font-trade-sans text-xs text-trade-muted">연결 상태</p>
            <div className="mt-2 flex items-center gap-2">
              <span className={`h-2 w-2 rounded-full ${statusDotColor(status)}`} />
              {isLoading ? (
                <span className="font-trade-sans text-sm text-trade-muted">확인 중…</span>
              ) : (
                <StatusBadge status={status} />
              )}
            </div>
            {status?.tokenExpiresAt && (
              <p className="mt-1 font-trade-mono text-xs text-trade-muted">
                토큰 만료: {fmtKst(status.tokenExpiresAt)}
              </p>
            )}
          </div>
          {status?.configured && (
            <TradeButton
              variant="secondary"
              size="sm"
              disabled={refreshMutation.isPending}
              onClick={() => {
                setMessage(null);
                refreshMutation.mutate();
              }}
            >
              {refreshMutation.isPending ? "갱신 중..." : "토큰 수동 갱신"}
            </TradeButton>
          )}
        </div>
      </TradeCard>

      {/* Credentials form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label
            htmlFor="clientId"
            className="block font-trade-sans text-sm font-medium text-trade-body"
          >
            Client ID
          </label>
          <TradeInput
            id="clientId"
            type="password"
            autoComplete="off"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            placeholder="토스증권 client_id"
            mono
            className="mt-1"
          />
        </div>
        <div>
          <label
            htmlFor="clientSecret"
            className="block font-trade-sans text-sm font-medium text-trade-body"
          >
            Client Secret
          </label>
          <TradeInput
            id="clientSecret"
            type="password"
            autoComplete="off"
            value={clientSecret}
            onChange={(e) => setClientSecret(e.target.value)}
            placeholder="토스증권 client_secret"
            mono
            className="mt-1"
          />
        </div>

        {message && (
          <p
            className={`font-trade-sans text-sm ${
              message.type === "success" ? "text-trade-up" : "text-trade-down"
            }`}
          >
            {message.text}
          </p>
        )}

        <TradeButton
          type="submit"
          variant="primary"
          disabled={!clientId.trim() || !clientSecret.trim() || saveMutation.isPending}
        >
          {saveMutation.isPending ? "저장 중..." : "자격증명 저장"}
        </TradeButton>
      </form>

      <p className="font-trade-sans text-xs text-trade-muted">
        토스증권 Open API는 토스증권 앱 → 서비스 → Open API에서 발급받을 수 있습니다.
      </p>
    </div>
  );
}
