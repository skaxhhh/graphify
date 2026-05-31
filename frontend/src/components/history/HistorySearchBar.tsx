interface HistorySearchBarProps {
  value: string;
  onChange: (value: string) => void;
}

export function HistorySearchBar({ value, onChange }: HistorySearchBarProps) {
  return (
    <label className="flex min-w-[200px] flex-1 flex-col gap-1">
      <span className="text-xs text-muted-gray">기업명 검색</span>
      <input
        type="search"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="기업명 또는 요약 검색"
        className="h-10 rounded-md border border-warm-border bg-cream px-3 text-sm text-charcoal placeholder:text-muted-gray"
      />
    </label>
  );
}
