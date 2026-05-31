import type { McpTool } from "@/types/mcpTool";

interface McpToolsTableProps {
  tools: McpTool[];
  pingLoadingId: number | null;
  pingMessageById: Record<number, string>;
  toggleLoadingId: number | null;
  onPing: (tool: McpTool) => void;
  onEdit: (tool: McpTool) => void;
  onDelete: (tool: McpTool) => void;
  onToggleEnabled: (tool: McpTool, enabled: boolean) => void;
}

function formatLastCalled(value: string | null): string {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function statusLabel(status: McpTool["status"]): string {
  const map: Record<McpTool["status"], string> = {
    CONNECTED: "연결됨",
    DISCONNECTED: "끊김",
    ERROR: "오류",
    UNKNOWN: "미확인",
  };
  return map[status] ?? status;
}

export function McpToolsTable({
  tools,
  pingLoadingId,
  pingMessageById,
  toggleLoadingId,
  onPing,
  onEdit,
  onDelete,
  onToggleEnabled,
}: McpToolsTableProps) {
  return (
    <div className="hidden overflow-x-auto rounded-xl border border-warm-border md:block">
      <table className="w-full min-w-[800px] text-left text-sm">
        <thead>
          <tr className="border-b border-warm-border text-xs text-muted-gray">
            <th className="px-4 py-3 font-medium">이름</th>
            <th className="px-4 py-3 font-medium">설명</th>
            <th className="px-4 py-3 font-medium">연결 상태</th>
            <th className="px-4 py-3 font-medium">마지막 호출</th>
            <th className="px-4 py-3 font-medium">활성</th>
            <th className="w-[180px] px-4 py-3 font-medium">액션</th>
          </tr>
        </thead>
        <tbody>
          {tools.map((tool) => (
            <tr key={tool.id} className="border-b border-warm-border/80">
              <td className="px-4 py-3 font-medium text-charcoal">{tool.name}</td>
              <td className="max-w-[240px] truncate px-4 py-3 text-muted-gray">
                {tool.description || "—"}
              </td>
              <td className="px-4 py-3">
                <span className="rounded-md border border-charcoal/20 px-2 py-0.5 text-xs text-muted-gray">
                  {statusLabel(tool.status)}
                </span>
                {pingMessageById[tool.id] ? (
                  <p className="mt-1 text-xs text-muted-gray">{pingMessageById[tool.id]}</p>
                ) : null}
              </td>
              <td className="px-4 py-3 text-muted-gray">
                {formatLastCalled(tool.lastCalledAt)}
              </td>
              <td className="px-4 py-3">
                <input
                  type="checkbox"
                  checked={tool.enabled}
                  disabled={toggleLoadingId === tool.id}
                  onChange={(e) => onToggleEnabled(tool, e.target.checked)}
                  aria-label={`${tool.name} 활성화`}
                />
              </td>
              <td className="px-4 py-3">
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => onPing(tool)}
                    disabled={pingLoadingId === tool.id}
                    className="rounded-md border border-warm-border px-2 py-1 text-xs hover:bg-light-cream/50"
                  >
                    {pingLoadingId === tool.id ? "Ping…" : "Ping"}
                  </button>
                  <button
                    type="button"
                    onClick={() => onEdit(tool)}
                    className="rounded-md border border-warm-border px-2 py-1 text-xs hover:bg-light-cream/50"
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(tool)}
                    className="rounded-md border border-warm-border px-2 py-1 text-xs text-red-800 hover:bg-red-50"
                  >
                    삭제
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
