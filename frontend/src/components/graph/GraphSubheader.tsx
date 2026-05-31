import { useNavigate } from "react-router-dom";
import { DepthStepper } from "@/components/shared/DepthStepper";
import { GhostButton } from "@/components/shared/GhostButton";
import type { Provenance } from "@/types/company";

interface GraphSubheaderProps {
  companyId: number;
  depth: number;
  onDepthChange: (depth: number) => void;
  onReset: () => void;
  provenance?: Provenance;
  filterSummary: string;
}

export function GraphSubheader({
  companyId,
  depth,
  onDepthChange,
  onReset,
  provenance,
  filterSummary,
}: GraphSubheaderProps) {
  const navigate = useNavigate();

  return (
    <header className="flex h-12 shrink-0 items-center gap-3 border-b border-warm-border bg-cream px-4">
      <button
        type="button"
        onClick={() => navigate(`/companies/${companyId}`)}
        className="text-sm text-charcoal hover:underline"
      >
        ← 상세
      </button>
      <DepthStepper value={depth} onChange={onDepthChange} />
      <span className="hidden truncate text-xs text-muted-gray sm:inline">{filterSummary}</span>
      <GhostButton className="ml-auto !h-9 !py-0 text-xs" onClick={onReset}>
        초기화
      </GhostButton>
      {provenance ? (
        <span
          className="hidden text-xs text-muted-gray lg:inline"
          title={`출처: ${provenance.sources.join(", ")}`}
        >
          데이터 출처 ⓘ
        </span>
      ) : null}
    </header>
  );
}
