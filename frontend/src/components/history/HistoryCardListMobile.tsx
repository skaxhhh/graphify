import { useNavigate } from "react-router-dom";
import type { HistoryItem } from "@/types/history";

const STATUS_LABEL: Record<string, string> = {
  COMPLETED: "완료",
  FAILED: "실패",
  RUNNING: "진행 중",
};

interface HistoryCardListMobileProps {
  items: HistoryItem[];
}

export function HistoryCardListMobile({ items }: HistoryCardListMobileProps) {
  const navigate = useNavigate();

  return (
    <ul className="space-y-3 md:hidden">
      {items.map((item) => (
        <li key={item.sessionId}>
          <button
            type="button"
            onClick={() => navigate(`/app/history/${item.sessionId}`)}
            className="w-full rounded-xl border border-warm-border bg-cream p-4 text-left transition-colors hover:bg-charcoal/[0.03]"
          >
            <div className="flex items-start justify-between gap-2">
              <p className="font-medium text-charcoal">{item.companyName}</p>
              <span className="shrink-0 rounded border border-warm-border px-2 py-0.5 text-xs text-muted-gray">
                {STATUS_LABEL[item.status] ?? item.status}
              </span>
            </div>
            <p className="mt-1 text-xs text-muted-gray">
              {new Date(item.analyzedAt).toLocaleString("ko-KR")}
            </p>
            {item.summaryLine ? (
              <p className="mt-2 line-clamp-2 text-sm text-muted-gray">{item.summaryLine}</p>
            ) : null}
          </button>
        </li>
      ))}
    </ul>
  );
}
