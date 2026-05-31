import { GhostButton } from "@/components/shared/GhostButton";
import type { AgentPromptVersion } from "@/types/agentPrompt";

interface VersionHistoryPanelProps {
  versions: AgentPromptVersion[];
  rollingBackId: number | null;
  onRollback: (version: AgentPromptVersion) => void;
}

function formatWhen(iso: string) {
  try {
    return new Date(iso).toLocaleString("ko-KR", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

export function VersionHistoryPanel({
  versions,
  rollingBackId,
  onRollback,
}: VersionHistoryPanelProps) {
  return (
    <aside className="flex min-h-0 w-full flex-col border-warm-border lg:w-[320px] lg:border-l">
      <div className="border-b border-warm-border px-4 py-3">
        <h2 className="text-sm font-semibold text-charcoal">버전 이력</h2>
        <p className="mt-0.5 text-xs text-muted-gray">최신순 · 롤백 시 새 버전 생성</p>
      </div>
      <ul className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
        {versions.length === 0 ? (
          <li className="text-sm text-muted-gray">저장된 버전이 없습니다.</li>
        ) : (
          versions.map((version) => (
            <li
              key={version.id}
              className="rounded-lg border border-warm-border bg-cream-surface p-3"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-charcoal">
                    v{version.versionNumber}
                  </p>
                  <p className="mt-1 text-xs text-muted-gray">
                    {formatWhen(version.createdAt)} · {version.author}
                  </p>
                  <p className="mt-2 line-clamp-2 text-xs text-charcoal">
                    {version.summary}
                  </p>
                </div>
              </div>
              <GhostButton
                type="button"
                className="mt-3 !w-full !py-2 text-xs"
                disabled={rollingBackId != null}
                onClick={() => onRollback(version)}
              >
                {rollingBackId === version.id ? "롤백 중…" : "이 버전으로 롤백"}
              </GhostButton>
            </li>
          ))
        )}
      </ul>
    </aside>
  );
}
