import { useNavigate } from "react-router-dom";
import type { CompanySearchItem } from "@/types/search";

interface CompanyResultRowProps {
  item: CompanySearchItem;
}

export function CompanyResultRow({ item }: CompanyResultRowProps) {
  const navigate = useNavigate();

  const goToDetail = () => {
    navigate(`/companies/${item.id}`);
  };

  return (
    <button
      type="button"
      onClick={goToDetail}
      onKeyDown={(event) => {
        if (event.key === "Enter") {
          goToDetail();
        }
      }}
      className="flex min-h-[72px] w-full items-center justify-between gap-4 border-b border-warm-border px-4 py-4 text-left transition-colors hover:bg-charcoal/[0.03]"
    >
      <div className="min-w-0 flex-1">
        <p className="truncate font-medium text-charcoal">{item.name}</p>
        <p className="mt-1 truncate text-xs text-muted-gray">
          {[item.ticker, item.industry, item.market].filter(Boolean).join(" · ")}
        </p>
      </div>
      <div className="shrink-0 text-right text-xs text-muted-gray">
        <span className="rounded border border-warm-border px-2 py-0.5">
          {item.dataFreshness}
        </span>
      </div>
    </button>
  );
}
