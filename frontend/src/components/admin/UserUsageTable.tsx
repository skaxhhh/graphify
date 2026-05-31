import { useMemo, useState } from "react";
import type { UserUsageRow } from "@/types/admin";

type SortKey = "name" | "requests" | "tokens" | "errors";

interface UserUsageTableProps {
  rows: UserUsageRow[];
  onRowClick: (row: UserUsageRow) => void;
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat("ko-KR").format(value);
}

export function UserUsageTable({ rows, onRowClick }: UserUsageTableProps) {
  const [sortKey, setSortKey] = useState<SortKey>("tokens");
  const [sortAsc, setSortAsc] = useState(false);

  const sortedRows = useMemo(() => {
    const copy = [...rows];
    copy.sort((a, b) => {
      let cmp = 0;
      if (sortKey === "name") {
        cmp = a.name.localeCompare(b.name, "ko");
      } else {
        cmp = a[sortKey] - b[sortKey];
      }
      return sortAsc ? cmp : -cmp;
    });
    return copy;
  }, [rows, sortAsc, sortKey]);

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortAsc((prev) => !prev);
      return;
    }
    setSortKey(key);
    setSortAsc(key === "name");
  };

  const sortIndicator = (key: SortKey) => {
    if (sortKey !== key) return "";
    return sortAsc ? " ↑" : " ↓";
  };

  return (
    <section className="rounded-xl border border-warm-border bg-cream p-4">
      <h2 className="text-base font-semibold text-charcoal">사용자별 사용량</h2>
      {rows.length === 0 ? (
        <p className="mt-6 text-sm text-muted-gray">
          집계할 사용자 사용량 데이터가 없습니다.
        </p>
      ) : (
        <div className="mt-3 min-w-0 overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead>
              <tr className="border-b border-warm-border text-muted-gray">
                <th className="px-2 py-2">
                  <button type="button" onClick={() => toggleSort("name")}>
                    사용자{sortIndicator("name")}
                  </button>
                </th>
                <th className="px-2 py-2">이메일</th>
                <th className="px-2 py-2 text-right">
                  <button type="button" onClick={() => toggleSort("requests")}>
                    요청{sortIndicator("requests")}
                  </button>
                </th>
                <th className="px-2 py-2 text-right">
                  <button type="button" onClick={() => toggleSort("tokens")}>
                    토큰{sortIndicator("tokens")}
                  </button>
                </th>
                <th className="px-2 py-2 text-right">
                  <button type="button" onClick={() => toggleSort("errors")}>
                    오류{sortIndicator("errors")}
                  </button>
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedRows.map((row) => (
                <tr
                  key={row.userId}
                  className="cursor-pointer border-b border-warm-border/80 transition-colors hover:bg-light-cream/50"
                  onClick={() => onRowClick(row)}
                >
                  <td className="px-2 py-2 font-medium text-charcoal">
                    {row.name}
                  </td>
                  <td className="px-2 py-2 text-muted-gray">{row.email}</td>
                  <td className="px-2 py-2 text-right">
                    {formatNumber(row.requests)}
                  </td>
                  <td className="px-2 py-2 text-right">
                    {formatNumber(row.tokens)}
                  </td>
                  <td className="px-2 py-2 text-right">
                    {formatNumber(row.errors)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
