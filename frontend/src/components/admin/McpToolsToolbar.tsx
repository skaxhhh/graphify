import type { McpConnectionStatus } from "@/types/mcpTool";

interface McpToolsToolbarProps {
  query: string;
  status: McpConnectionStatus | "ALL";
  onQueryChange: (value: string) => void;
  onStatusChange: (value: McpConnectionStatus | "ALL") => void;
}

const STATUS_OPTIONS: Array<{ value: McpConnectionStatus | "ALL"; label: string }> =
  [
    { value: "ALL", label: "전체 상태" },
    { value: "CONNECTED", label: "연결됨" },
    { value: "DISCONNECTED", label: "끊김" },
    { value: "ERROR", label: "오류" },
    { value: "UNKNOWN", label: "미확인" },
  ];

export function McpToolsToolbar({
  query,
  status,
  onQueryChange,
  onStatusChange,
}: McpToolsToolbarProps) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <input
        type="search"
        value={query}
        onChange={(e) => onQueryChange(e.target.value)}
        placeholder="도구 이름·설명 검색"
        className="h-11 w-full flex-1 rounded-md border border-warm-border bg-cream px-3 text-sm text-charcoal outline-none focus:border-charcoal/40 sm:max-w-md"
        aria-label="MCP 도구 검색"
      />
      <select
        value={status}
        onChange={(e) =>
          onStatusChange(e.target.value as McpConnectionStatus | "ALL")
        }
        className="h-11 w-full rounded-md border border-warm-border bg-cream px-3 text-sm text-charcoal sm:w-40"
        aria-label="연결 상태 필터"
      >
        {STATUS_OPTIONS.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  );
}
