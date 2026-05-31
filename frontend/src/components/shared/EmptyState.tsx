import { Link } from "react-router-dom";

interface EmptyStateProps {
  title: string;
  description?: string;
  suggestions?: string[];
  onSuggestionClick?: (query: string) => void;
}

export function EmptyState({
  title,
  description,
  suggestions = [],
  onSuggestionClick,
}: EmptyStateProps) {
  return (
    <div className="flex min-h-[320px] flex-col items-center justify-center rounded-xl border border-warm-border bg-cream px-6 py-12 text-center">
      <p className="text-lg font-medium text-charcoal">{title}</p>
      {description ? (
        <p className="mt-2 max-w-md text-sm text-muted-gray">{description}</p>
      ) : null}
      {suggestions.length > 0 ? (
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          {suggestions.map((suggestion) => (
            <button
              key={suggestion}
              type="button"
              onClick={() => onSuggestionClick?.(suggestion)}
              className="rounded-full border border-warm-border px-3 py-1 text-xs text-charcoal hover:bg-charcoal/[0.03]"
            >
              {suggestion}
            </button>
          ))}
        </div>
      ) : null}
      <Link
        to="/"
        className="mt-8 text-sm text-charcoal underline underline-offset-2"
      >
        홈으로 돌아가기
      </Link>
    </div>
  );
}
