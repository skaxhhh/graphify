import { useEffect, useRef, useState } from "react";

export interface SearchFilters {
  industry: string;
  market: string;
  dataStatus: string;
}

interface FilterPopoverProps {
  value: SearchFilters;
  onChange: (value: SearchFilters) => void;
  disabled?: boolean;
}

const industries = ["반도체", "2차전지", "자동차", "인터넷", "바이오", "철강"];
const markets = ["KOSPI", "KOSDAQ"];
const dataStatuses = ["FRESH", "STALE"];

export function FilterPopover({
  value,
  onChange,
  disabled = false,
}: FilterPopoverProps) {
  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const handleClick = (event: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  const activeCount = [value.industry, value.market, value.dataStatus].filter(Boolean)
    .length;

  return (
    <div ref={panelRef} className="relative w-full md:w-auto">
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen((prev) => !prev)}
        className="flex h-10 w-full items-center justify-between rounded-md border border-warm-border bg-cream px-3 text-sm text-charcoal hover:opacity-90 disabled:opacity-60 md:min-w-[140px]"
      >
        <span>필터{activeCount > 0 ? ` (${activeCount})` : ""}</span>
        <span aria-hidden>{open ? "▲" : "▼"}</span>
      </button>

      {open ? (
        <div className="absolute right-0 z-40 mt-2 max-h-[70vh] w-full min-w-[280px] overflow-y-auto rounded-lg border border-warm-border bg-cream p-4 shadow-focus md:w-[280px]">
          <FilterField
            label="업종"
            value={value.industry}
            options={industries}
            onSelect={(industry) => onChange({ ...value, industry })}
          />
          <FilterField
            label="시장"
            value={value.market}
            options={markets}
            onSelect={(market) => onChange({ ...value, market })}
          />
          <FilterField
            label="데이터 상태"
            value={value.dataStatus}
            options={dataStatuses}
            onSelect={(dataStatus) => onChange({ ...value, dataStatus })}
          />
          <button
            type="button"
            className="mt-4 w-full text-sm text-muted-gray underline hover:text-charcoal"
            onClick={() => onChange({ industry: "", market: "", dataStatus: "" })}
          >
            필터 초기화
          </button>
        </div>
      ) : null}
    </div>
  );
}

function FilterField({
  label,
  value,
  options,
  onSelect,
}: {
  label: string;
  value: string;
  options: string[];
  onSelect: (next: string) => void;
}) {
  return (
    <div className="mt-3 first:mt-0">
      <p className="text-xs text-muted-gray">{label}</p>
      <div className="mt-2 flex flex-wrap gap-2">
        <Chip label="전체" active={!value} onClick={() => onSelect("")} />
        {options.map((option) => (
          <Chip
            key={option}
            label={option}
            active={value === option}
            onClick={() => onSelect(value === option ? "" : option)}
          />
        ))}
      </div>
    </div>
  );
}

function Chip({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full border px-3 py-1 text-xs transition-colors ${
        active
          ? "border-charcoal bg-charcoal text-off-white"
          : "border-warm-border text-charcoal hover:bg-charcoal/[0.03]"
      }`}
    >
      {label}
    </button>
  );
}
