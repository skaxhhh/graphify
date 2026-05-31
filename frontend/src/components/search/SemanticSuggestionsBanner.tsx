import { Link } from "react-router-dom";
import type { SemanticHints } from "@/types/search";

interface SemanticSuggestionsBannerProps {
  hints: SemanticHints;
  onQuerySelect: (query: string) => void;
}

export function SemanticSuggestionsBanner({
  hints,
  onQuerySelect,
}: SemanticSuggestionsBannerProps) {
  const hasQueries = hints.relatedQueries.length > 0;
  const hasSimilar = hints.similarCompanies.length > 0;

  if (!hasQueries && !hasSimilar) {
    return null;
  }

  return (
    <div className="rounded-lg border border-warm-border bg-cream px-4 py-3">
      <p className="text-sm text-charcoal">유사 검색 제안</p>
      {hasQueries ? (
        <div className="mt-2 flex flex-wrap gap-2">
          {hints.relatedQueries.map((query) => (
            <button
              key={query}
              type="button"
              onClick={() => onQuerySelect(query)}
              className="rounded-full border border-warm-border px-3 py-1 text-xs text-charcoal hover:bg-charcoal/[0.03]"
            >
              {query}
            </button>
          ))}
        </div>
      ) : null}
      {hasSimilar ? (
        <ul className="mt-3 space-y-2">
          {hints.similarCompanies.map((company) => (
            <li key={company.id}>
              <Link
                to={`/companies/${company.id}`}
                className="text-sm text-charcoal underline underline-offset-2"
              >
                {company.name}
                {company.ticker ? ` (${company.ticker})` : ""}
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
