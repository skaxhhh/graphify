import type { CompareBasis } from "@/types/watchlist";

const TABS: { id: CompareBasis; label: string }[] = [
  { id: "INVESTMENT", label: "투자" },
  { id: "SUPPLY_CHAIN", label: "공급망" },
  { id: "PARTNERSHIP", label: "협력" },
];

interface BasisTabsProps {
  value: CompareBasis;
  onChange: (basis: CompareBasis) => void;
}

export function BasisTabs({ value, onChange }: BasisTabsProps) {
  return (
    <div className="flex gap-1 border-b border-warm-border" role="tablist">
      {TABS.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={value === tab.id}
          onClick={() => onChange(tab.id)}
          className={`px-4 py-2 text-sm transition-colors ${
            value === tab.id
              ? "border-b-2 border-charcoal font-medium text-charcoal"
              : "text-muted-gray hover:text-charcoal"
          }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
