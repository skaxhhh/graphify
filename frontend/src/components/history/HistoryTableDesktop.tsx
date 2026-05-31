import { useNavigate } from "react-router-dom";
import type { HistoryItem } from "@/types/history";

const STATUS_LABEL: Record<string, string> = {
  COMPLETED: "완료",
  FAILED: "실패",
  RUNNING: "진행 중",
};

interface HistoryTableDesktopProps {
  items: HistoryItem[];
}

export function HistoryTableDesktop({ items }: HistoryTableDesktopProps) {
  const navigate = useNavigate();

  return (
    <div className="hidden overflow-hidden rounded-xl border border-warm-border bg-cream md:block">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-warm-border text-xs text-muted-gray">
            <th className="px-4 py-3 font-medium">기업명</th>
            <th className="px-4 py-3 font-medium">분석 일시</th>
            <th className="px-4 py-3 font-medium">상태</th>
            <th className="px-4 py-3 font-medium">한줄 요약</th>
            <th className="px-4 py-3 font-medium">액션</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr
              key={item.sessionId}
              className="min-h-[56px] cursor-pointer border-b border-warm-border last:border-0 hover:bg-charcoal/[0.04]"
              onClick={() => navigate(`/app/history/${item.sessionId}`)}
            >
              <td className="px-4 py-4 font-medium text-charcoal">{item.companyName}</td>
              <td className="px-4 py-4 text-muted-gray">
                {new Date(item.analyzedAt).toLocaleString("ko-KR")}
              </td>
              <td className="px-4 py-4">
                <span className="rounded border border-warm-border px-2 py-0.5 text-xs text-muted-gray">
                  {STATUS_LABEL[item.status] ?? item.status}
                </span>
              </td>
              <td className="max-w-xs truncate px-4 py-4 text-muted-gray">
                {item.summaryLine ?? "—"}
              </td>
              <td className="px-4 py-4 text-charcoal underline">상세</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
