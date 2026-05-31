import { useQuery } from "@tanstack/react-query";
import { fetchCompanySearch } from "@/lib/searchApi";
import type { ReindexScope } from "@/types/vectorDb";

interface EntityMultiSelectProps {
  scope: ReindexScope;
  selectedIds: number[];
  onChange: (ids: number[]) => void;
  disabled?: boolean;
}

export function EntityMultiSelect({
  scope,
  selectedIds,
  onChange,
  disabled = false,
}: EntityMultiSelectProps) {
  const searchQuery = useQuery({
    queryKey: ["admin", "vectordb", "company-search"],
    queryFn: () => fetchCompanySearch({ q: "a", page: 0, size: 20 }),
    enabled: scope === "SELECTED",
    staleTime: 60_000,
  });

  if (scope !== "SELECTED") {
    return null;
  }

  const companies = searchQuery.data?.data?.items ?? [];

  const toggle = (id: number) => {
    if (disabled) return;
    if (selectedIds.includes(id)) {
      onChange(selectedIds.filter((x) => x !== id));
    } else {
      onChange([...selectedIds, id]);
    }
  };

  return (
    <div className="space-y-2">
      <p className="text-sm font-medium text-charcoal">대상 기업 선택</p>
      {searchQuery.isLoading ? (
        <p className="text-sm text-muted-gray">기업 목록을 불러오는 중…</p>
      ) : companies.length === 0 ? (
        <p className="text-sm text-muted-gray">선택 가능한 기업이 없습니다.</p>
      ) : (
        <ul className="max-h-40 space-y-1 overflow-y-auto rounded-lg border border-warm-border p-2">
          {companies.map((c) => (
            <li key={c.id}>
              <label className="flex cursor-pointer items-center gap-2 rounded px-2 py-1.5 text-sm hover:bg-light-cream/60">
                <input
                  type="checkbox"
                  className="accent-charcoal"
                  checked={selectedIds.includes(c.id)}
                  disabled={disabled}
                  onChange={() => toggle(c.id)}
                />
                <span className="text-charcoal">{c.name}</span>
                {c.ticker ? (
                  <span className="text-muted-gray">({c.ticker})</span>
                ) : null}
              </label>
            </li>
          ))}
        </ul>
      )}
      {selectedIds.length === 0 ? (
        <p className="text-xs text-muted-gray" role="status">
          재임베딩할 기업을 하나 이상 선택하세요.
        </p>
      ) : (
        <p className="text-xs text-muted-gray">{selectedIds.length}개 선택됨</p>
      )}
    </div>
  );
}
