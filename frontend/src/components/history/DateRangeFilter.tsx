export interface DateRange {
  from: string;
  to: string;
}

interface DateRangeFilterProps {
  value: DateRange;
  onChange: (value: DateRange) => void;
}

export function DateRangeFilter({ value, onChange }: DateRangeFilterProps) {
  return (
    <div className="flex flex-wrap items-end gap-3">
      <label className="flex flex-col gap-1">
        <span className="text-xs text-muted-gray">시작일</span>
        <input
          type="date"
          value={value.from}
          onChange={(event) => onChange({ ...value, from: event.target.value })}
          className="h-10 rounded-md border border-warm-border bg-cream px-3 text-sm text-charcoal"
        />
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-xs text-muted-gray">종료일</span>
        <input
          type="date"
          value={value.to}
          onChange={(event) => onChange({ ...value, to: event.target.value })}
          className="h-10 rounded-md border border-warm-border bg-cream px-3 text-sm text-charcoal"
        />
      </label>
    </div>
  );
}
