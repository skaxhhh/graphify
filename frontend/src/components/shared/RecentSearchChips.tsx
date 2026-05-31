import { useNavigate } from "react-router-dom";
import { useRecentSearches } from "@/hooks/useRecentSearches";

export function RecentSearchChips() {
  const navigate = useNavigate();
  const { entries, removeEntry, clearAll } = useRecentSearches();

  if (entries.length === 0) {
    return null;
  }

  return (
    <section className="mt-6 w-full" aria-label="최근 검색">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-medium text-charcoal">최근 검색</h2>
        <button
          type="button"
          onClick={clearAll}
          className="text-xs text-muted-gray underline hover:text-charcoal"
        >
          전체 삭제
        </button>
      </div>
      <ul className="flex flex-wrap justify-center gap-2">
        {entries.map((entry) => (
          <li key={entry.companyId}>
            <span className="inline-flex items-center gap-1 rounded-full border border-warm-border bg-cream px-3 py-1.5 text-sm text-charcoal">
              <button
                type="button"
                onClick={() => navigate(`/companies/${entry.companyId}`)}
                className="hover:underline"
              >
                {entry.label}
              </button>
              <button
                type="button"
                aria-label={`${entry.label} 삭제`}
                onClick={() => removeEntry(entry.companyId)}
                className="ml-1 text-muted-gray hover:text-charcoal"
              >
                ×
              </button>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
