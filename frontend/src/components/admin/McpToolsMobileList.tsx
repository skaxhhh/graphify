import type { McpTool } from "@/types/mcpTool";

interface McpToolsMobileListProps {
  tools: McpTool[];
  onEdit: (tool: McpTool) => void;
  onDelete: (tool: McpTool) => void;
  onPing: (tool: McpTool) => void;
}

export function McpToolsMobileList({
  tools,
  onEdit,
  onDelete,
  onPing,
}: McpToolsMobileListProps) {
  return (
    <ul className="space-y-3 md:hidden">
      {tools.map((tool) => (
        <li
          key={tool.id}
          className="rounded-xl border border-warm-border bg-cream p-4 shadow-sm"
        >
          <div className="flex items-start justify-between gap-2">
            <div>
              <p className="font-medium text-charcoal">{tool.name}</p>
              <p className="mt-1 text-xs text-muted-gray">{tool.status}</p>
            </div>
            <span
              className={`text-xs ${tool.enabled ? "text-charcoal" : "text-muted-gray"}`}
            >
              {tool.enabled ? "ON" : "OFF"}
            </span>
          </div>
          <p className="mt-2 line-clamp-2 text-sm text-muted-gray">
            {tool.description || "설명 없음"}
          </p>
          <div className="mt-3 flex gap-2">
            <button
              type="button"
              onClick={() => onPing(tool)}
              className="flex-1 rounded-md border border-warm-border py-2 text-xs"
            >
              Ping
            </button>
            <button
              type="button"
              onClick={() => onEdit(tool)}
              className="flex-1 rounded-md border border-warm-border py-2 text-xs"
            >
              수정
            </button>
            <button
              type="button"
              onClick={() => onDelete(tool)}
              className="flex-1 rounded-md border border-warm-border py-2 text-xs text-red-800"
            >
              삭제
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
