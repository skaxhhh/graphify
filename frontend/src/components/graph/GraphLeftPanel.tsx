import { RelationTypeFilter } from "@/components/graph/RelationTypeFilter";
import type { DimMode, RelationType } from "@/types/graph";

interface GraphLeftPanelProps {
  selectedTypes: RelationType[];
  onTypesChange: (types: RelationType[]) => void;
  dimMode: DimMode;
  onDimModeChange: (mode: DimMode) => void;
}

export function GraphLeftPanel({
  selectedTypes,
  onTypesChange,
  dimMode,
  onDimModeChange,
}: GraphLeftPanelProps) {
  return (
    <aside className="flex h-full min-h-0 w-72 shrink-0 flex-col gap-6 overflow-y-auto border-r border-warm-border bg-cream p-4">
      <RelationTypeFilter selected={selectedTypes} onChange={onTypesChange} />
      <section className="space-y-2">
        <h3 className="text-sm font-semibold text-charcoal">비선택 표시</h3>
        <label className="flex items-center gap-2 text-sm text-charcoal">
          <input
            type="radio"
            name="dimMode"
            checked={dimMode === "dim"}
            onChange={() => onDimModeChange("dim")}
          />
          흐리게
        </label>
        <label className="flex items-center gap-2 text-sm text-charcoal">
          <input
            type="radio"
            name="dimMode"
            checked={dimMode === "hide"}
            onChange={() => onDimModeChange("hide")}
          />
          숨김
        </label>
      </section>
      <p className="text-[11px] text-muted-gray">
        필터·깊이 설정은 세션 동안 유지됩니다.
      </p>
    </aside>
  );
}
