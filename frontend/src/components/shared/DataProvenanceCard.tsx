import type { Provenance } from "@/types/company";

interface DataProvenanceCardProps {
  provenance: Provenance;
  onReliabilityClick?: () => void;
}

export function DataProvenanceCard({ provenance, onReliabilityClick }: DataProvenanceCardProps) {
  return (
    <div className="rounded-xl border border-warm-border bg-cream p-5">
      <h3 className="text-sm font-semibold text-charcoal">데이터 출처</h3>
      <ul className="mt-3 space-y-2 text-sm text-muted-gray">
        <li>
          <span className="text-charcoal">출처: </span>
          {provenance.sources.join(", ")}
        </li>
        <li>
          <span className="text-charcoal">MCP: </span>
          {provenance.mcpToolsUsed.join(", ")}
        </li>
        <li>
          <span className="text-charcoal">최종 갱신: </span>
          {new Date(provenance.lastUpdated).toLocaleString("ko-KR")}
        </li>
      </ul>
      {onReliabilityClick ? (
        <button
          type="button"
          onClick={onReliabilityClick}
          className="mt-4 text-xs text-muted-gray underline hover:text-charcoal"
        >
          신뢰도 기준 보기
        </button>
      ) : null}
    </div>
  );
}
