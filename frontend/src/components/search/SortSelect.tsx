import type { CompanySort } from "@/types/search";

interface SortSelectProps {
  value: CompanySort;
  onChange: (value: CompanySort) => void;
  disabled?: boolean;
}

const options: { value: CompanySort; label: string }[] = [
  { value: "name", label: "이름순" },
  { value: "industry", label: "업종순" },
  { value: "updatedAt", label: "최신순" },
];

export function SortSelect({ value, onChange, disabled = false }: SortSelectProps) {
  return (
    <label className="flex w-full flex-col gap-1 md:w-auto">
      <span className="text-xs text-muted-gray">정렬</span>
      <select
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value as CompanySort)}
        className="h-10 w-full rounded-md border border-warm-border bg-cream px-3 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue disabled:opacity-60 md:min-w-[140px]"
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}
