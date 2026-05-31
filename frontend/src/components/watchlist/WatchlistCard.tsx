import { Link } from "react-router-dom";
import type { WatchlistItem } from "@/types/watchlist";

interface WatchlistCardProps {
  item: WatchlistItem;
  selected: boolean;
  onToggleCompare: (companyId: number) => void;
  onRemove: (companyId: number) => void;
  removing?: boolean;
}

export function WatchlistCard({
  item,
  selected,
  onToggleCompare,
  onRemove,
  removing = false,
}: WatchlistCardProps) {
  return (
    <article className="flex flex-col gap-4 rounded-xl border border-warm-border bg-cream p-4 md:p-6">
      <div className="flex items-start justify-between gap-3">
        <label className="flex cursor-pointer items-center gap-2 text-sm text-charcoal">
          <input
            type="checkbox"
            checked={selected}
            onChange={() => onToggleCompare(item.companyId)}
            className="h-4 w-4 rounded border-warm-border"
          />
          비교
        </label>
        <button
          type="button"
          disabled={removing}
          onClick={() => onRemove(item.companyId)}
          className="text-xs text-muted-gray underline hover:text-charcoal disabled:opacity-50"
        >
          관심 해제
        </button>
      </div>
      <div>
        <h2 className="text-lg font-semibold text-charcoal">{item.name}</h2>
        <p className="mt-1 text-xs text-muted-gray">
          {item.industry ?? "업종 미분류"}
          {item.ticker ? ` · ${item.ticker}` : ""}
        </p>
        <p className="mt-2 text-xs text-muted-gray">
          등록 {new Date(item.addedAt).toLocaleDateString("ko-KR")}
        </p>
      </div>
      <div className="mt-auto flex gap-3 text-xs text-muted-gray">
        <span>인사이트·관계 데이터 연동</span>
      </div>
      <Link
        to={`/companies/${item.companyId}`}
        className="text-sm text-charcoal underline hover:opacity-80"
      >
        기업 상세 보기
      </Link>
    </article>
  );
}
