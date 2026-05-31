import { CompanyResultRow } from "@/components/search/CompanyResultRow";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import type { CompanySearchItem } from "@/types/search";

interface CompanyResultListProps {
  items: CompanySearchItem[];
  loading?: boolean;
}

export function CompanyResultList({ items, loading = false }: CompanyResultListProps) {
  if (loading) {
    return (
      <div className="min-h-[320px] rounded-xl border border-warm-border bg-cream">
        {Array.from({ length: 8 }).map((_, index) => (
          <div
            key={index}
            className="flex min-h-[72px] items-center gap-4 border-b border-warm-border px-4 py-4"
          >
            <div className="flex-1 space-y-2">
              <SkeletonBlock className="h-4 w-1/3" />
              <SkeletonBlock className="h-3 w-1/4" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="min-h-[320px] overflow-hidden rounded-xl border border-warm-border bg-cream">
      {items.map((item) => (
        <CompanyResultRow key={item.id} item={item} />
      ))}
    </div>
  );
}
