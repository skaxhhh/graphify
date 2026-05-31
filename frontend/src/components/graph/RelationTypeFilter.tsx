import type { RelationType } from "@/types/graph";

const RELATION_OPTIONS: { value: RelationType; label: string }[] = [
  { value: "SUPPLY_CHAIN", label: "공급망" },
  { value: "INVESTMENT", label: "투자" },
  { value: "PARTNERSHIP", label: "파트너십" },
  { value: "RISK", label: "리스크" },
];

interface RelationTypeFilterProps {
  selected: RelationType[];
  onChange: (next: RelationType[]) => void;
}

export function RelationTypeFilter({ selected, onChange }: RelationTypeFilterProps) {
  const toggle = (type: RelationType) => {
    if (selected.includes(type)) {
      onChange(selected.filter((item) => item !== type));
    } else {
      onChange([...selected, type]);
    }
  };

  const selectAll = () => onChange(RELATION_OPTIONS.map((o) => o.value));

  return (
    <section className="space-y-3">
      <header className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-charcoal">관계 유형</h3>
        <button
          type="button"
          onClick={selectAll}
          className="text-xs text-muted-gray underline hover:text-charcoal"
        >
          전체
        </button>
      </header>
      <ul className="flex flex-col gap-2">
        {RELATION_OPTIONS.map((option) => (
          <li key={option.value}>
            <label className="flex cursor-pointer items-center gap-2 text-sm text-charcoal">
              <input
                type="checkbox"
                checked={selected.includes(option.value)}
                onChange={() => toggle(option.value)}
                className="rounded border-warm-border"
              />
              {option.label}
            </label>
          </li>
        ))}
      </ul>
    </section>
  );
}
