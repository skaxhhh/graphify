import { FilterPopover, type SearchFilters } from "@/components/search/FilterPopover";
import { SortSelect } from "@/components/search/SortSelect";
import type { CompanySort } from "@/types/search";

interface SearchToolbarProps {
  total: number;
  sort: CompanySort;
  filters: SearchFilters;
  filtering?: boolean;
  onSortChange: (sort: CompanySort) => void;
  onFiltersChange: (filters: SearchFilters) => void;
}

export function SearchToolbar({
  total,
  sort,
  filters,
  filtering = false,
  onSortChange,
  onFiltersChange,
}: SearchToolbarProps) {
  return (
    <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <p className="text-sm text-muted-gray">
        검색 결과 <span className="text-charcoal">{total}</span>건
        {filtering ? (
          <span className="ml-2 inline-block h-3 w-3 animate-spin rounded-full border-2 border-muted-gray/30 border-t-charcoal align-middle" />
        ) : null}
      </p>
      <div className="flex flex-col gap-3 sm:flex-row md:items-end">
        <SortSelect value={sort} onChange={onSortChange} disabled={filtering} />
        <FilterPopover
          value={filters}
          onChange={onFiltersChange}
          disabled={filtering}
        />
      </div>
    </div>
  );
}
