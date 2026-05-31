import type { ReactNode } from "react";
import type { HistoryStatus } from "@/types/history";

const STATUS_LABEL: Record<HistoryStatus, string> = {
  COMPLETED: "완료",
  FAILED: "실패",
  RUNNING: "진행 중",
};

interface SessionMetaBarProps {
  companyName: string;
  analyzedAt: string;
  status: HistoryStatus;
  compareToggle: ReactNode;
}

export function SessionMetaBar({
  companyName,
  analyzedAt,
  status,
  compareToggle,
}: SessionMetaBarProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div className="min-w-0 flex-1">
        <h1 className="text-xl font-semibold text-charcoal md:text-2xl">{companyName}</h1>
        <p className="mt-1 text-xs text-muted-gray md:text-sm">
          분석 일시 {new Date(analyzedAt).toLocaleString("ko-KR")}
        </p>
        <span className="mt-2 inline-block rounded border border-warm-border px-2 py-0.5 text-xs text-muted-gray">
          {STATUS_LABEL[status]}
        </span>
      </div>
      {compareToggle}
    </div>
  );
}
