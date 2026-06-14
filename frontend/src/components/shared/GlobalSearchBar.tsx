import { useCallback, useEffect, useId, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { InlineError } from "@/components/shared/InlineError";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { useDebounce } from "@/hooks/useDebounce";
import { fetchAutocomplete } from "@/lib/searchApi";
import { useAuthStore } from "@/stores/authStore";
import { useTradingStore } from "@/stores/tradingStore";
import { fetchUserMe } from "@/lib/userApi";
import type { AutocompleteItem } from "@/types/search";

const MIN_QUERY_LENGTH = 2;
const DEBOUNCE_MS = 250;

interface GlobalSearchBarProps {
  variant?: "hero" | "inline";
  initialQuery?: string;
  onCompanySelect?: (item: AutocompleteItem) => void;
}

export function GlobalSearchBar({
  variant = "hero",
  initialQuery = "",
  onCompanySelect,
}: GlobalSearchBarProps) {
  const navigate = useNavigate();
  const enableDarkMode = useTradingStore((s) => s.enableDarkMode);
  const user = useAuthStore((s) => s.user);
  const listboxId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState(initialQuery);

  useEffect(() => {
    setQuery(initialQuery);
  }, [initialQuery]);
  const [isFocused, setIsFocused] = useState(false);
  const [highlightIndex, setHighlightIndex] = useState(-1);

  const debouncedQuery = useDebounce(query, DEBOUNCE_MS);
  const shouldFetch =
    debouncedQuery.trim().length >= MIN_QUERY_LENGTH && isFocused;

  const autocompleteQuery = useQuery({
    queryKey: ["search", "autocomplete", debouncedQuery],
    queryFn: async () => {
      const response = await fetchAutocomplete(debouncedQuery);
      return response.data ?? [];
    },
    enabled: shouldFetch,
  });

  const items = autocompleteQuery.data ?? [];
  const showDropdown = isFocused && query.trim().length >= MIN_QUERY_LENGTH;

  const goToSearchResults = useCallback(
    (q: string) => {
      const trimmed = q.trim();
      if (!trimmed) return;
      navigate(`/search?q=${encodeURIComponent(trimmed)}`);
    },
    [navigate]
  );

  const goToCompany = useCallback(
    (item: AutocompleteItem) => {
      onCompanySelect?.(item);
      navigate(`/companies/${item.id}`);
    },
    [navigate, onCompanySelect]
  );

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (query.trim() === "/gg") {
      if (user) {
        void fetchUserMe().then((res) => {
          if (res.data?.tradingEnabled) {
            enableDarkMode();
            navigate("/trading");
          }
        });
      }
      return;
    }
    if (highlightIndex >= 0 && items[highlightIndex]) {
      goToCompany(items[highlightIndex]);
      return;
    }
    goToSearchResults(query);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (!showDropdown) return;

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setHighlightIndex((prev) =>
        items.length === 0 ? -1 : Math.min(prev + 1, items.length - 1)
      );
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setHighlightIndex((prev) => Math.max(prev - 1, -1));
    } else if (event.key === "Escape") {
      setIsFocused(false);
      setHighlightIndex(-1);
    }
  };

  useEffect(() => {
    setHighlightIndex(-1);
  }, [debouncedQuery, items.length]);

  const inputClassName =
    variant === "hero"
      ? "flex-1 rounded-md border border-warm-border bg-cream px-4 py-3 text-base text-charcoal placeholder:text-muted-gray focus:outline-none focus:ring-2 focus:ring-ring-blue"
      : "flex-1 rounded-md border border-warm-border bg-cream px-3 py-2 text-sm text-charcoal placeholder:text-muted-gray focus:outline-none focus:ring-2 focus:ring-ring-blue";

  return (
    <div className="relative w-full">
      <form
        onSubmit={handleSubmit}
        className={
          variant === "hero"
            ? "flex flex-col gap-3 sm:flex-row"
            : "flex gap-2"
        }
      >
        <input
          ref={inputRef}
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => setIsFocused(true)}
          onBlur={() => {
            window.setTimeout(() => setIsFocused(false), 150);
          }}
          onKeyDown={handleKeyDown}
          placeholder="기업명 또는 종목코드 검색"
          className={inputClassName}
          role="combobox"
          aria-expanded={showDropdown}
          aria-controls={listboxId}
          aria-autocomplete="list"
          autoComplete="off"
        />
        <button
          type="submit"
          className="shrink-0 rounded-md bg-charcoal px-6 py-3 text-sm text-off-white shadow-btn-inset transition-opacity hover:opacity-90 sm:py-3"
        >
          검색
        </button>
      </form>

      {showDropdown ? (
        <div
          id={listboxId}
          role="listbox"
          className="absolute left-0 right-0 top-full z-50 mt-2 overflow-hidden rounded-lg border border-warm-border bg-cream shadow-focus"
        >
          {autocompleteQuery.isLoading ? (
            <div className="space-y-2 p-3">
              <SkeletonBlock className="h-4 w-full" />
              <SkeletonBlock className="h-4 w-5/6" />
              <SkeletonBlock className="h-4 w-4/6" />
            </div>
          ) : null}

          {autocompleteQuery.isError ? (
            <InlineError
              message="자동완성을 불러오지 못했습니다."
              onRetry={() => void autocompleteQuery.refetch()}
            />
          ) : null}

          {!autocompleteQuery.isLoading &&
          !autocompleteQuery.isError &&
          items.length === 0 ? (
            <div className="px-4 py-3 text-sm text-muted-gray">
              <p>일치하는 기업이 없습니다.</p>
              <button
                type="button"
                className="mt-2 underline text-charcoal"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => goToSearchResults(query)}
              >
                전체 검색
              </button>
            </div>
          ) : null}

          {!autocompleteQuery.isLoading &&
          !autocompleteQuery.isError &&
          items.length > 0
            ? items.map((item, index) => (
                <button
                  key={item.id}
                  type="button"
                  role="option"
                  aria-selected={highlightIndex === index}
                  className={`flex w-full items-center justify-between px-4 py-3 text-left text-sm transition-colors ${
                    highlightIndex === index
                      ? "bg-charcoal/5 text-charcoal"
                      : "text-charcoal hover:bg-charcoal/[0.03]"
                  }`}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => goToCompany(item)}
                >
                  <span className="font-medium">{item.name}</span>
                  <span className="text-xs text-muted-gray">
                    {item.ticker ?? item.matchType}
                  </span>
                </button>
              ))
            : null}
        </div>
      ) : null}
    </div>
  );
}
