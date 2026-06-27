import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchCompanySearch } from "@/lib/searchApi";
import { ApiRequestError } from "@/lib/apiClient";
import { useDebounce } from "@/hooks/useDebounce";
import { TextField } from "@/components/shared/TextField";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { GhostButton } from "@/components/shared/GhostButton";
import { EmptyState } from "@/components/shared/EmptyState";
import { InlineError } from "@/components/shared/InlineError";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { Pagination } from "@/components/shared/Pagination";

const PAGE_SIZE = 10;

interface SelectedCompany {
  symbol: string;
  name: string;
}

interface CompanyPickerModalProps {
  open: boolean;
  /** 모달 제목 */
  title?: string;
  /** 부제/안내 문구 (보통 폴백 원인 메시지) */
  description?: string;
  /** 확정 버튼 라벨 */
  confirmLabel?: string;
  /** 선택 종목 심볼 배열을 돌려준다 */
  onConfirm: (symbols: string[]) => void;
  onClose: () => void;
}

export function CompanyPickerModal({
  open,
  title = "종목 직접 선택",
  description,
  confirmLabel = "선택 완료",
  onConfirm,
  onClose,
}: CompanyPickerModalProps) {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<SelectedCompany[]>([]);

  const debouncedQuery = useDebounce(query.trim(), 300);
  const dialogRef = useRef<HTMLDivElement>(null);

  // 모달이 열릴 때마다 상태 초기화 + 검색 입력에 포커스
  useEffect(() => {
    if (!open) return;
    setQuery("");
    setPage(0);
    setSelected([]);
    const id = window.setTimeout(
      () => dialogRef.current?.querySelector("input")?.focus(),
      0
    );
    return () => window.clearTimeout(id);
  }, [open]);

  // 검색어 변경 시 첫 페이지로
  useEffect(() => {
    setPage(0);
  }, [debouncedQuery]);

  // Esc 닫기 + 포커스 트랩
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
        return;
      }
      if (event.key !== "Tab") return;
      const root = dialogRef.current;
      if (!root) return;
      const focusable = root.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), textarea, input, select, [tabindex]:not([tabindex="-1"])'
      );
      if (focusable.length === 0) return;
      const first = focusable[0]!;
      const last = focusable[focusable.length - 1]!;
      const active = document.activeElement;
      if (event.shiftKey) {
        if (active === first || !root.contains(active)) {
          event.preventDefault();
          last.focus();
        }
      } else if (active === last) {
        event.preventDefault();
        first.focus();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  const searchQuery = useQuery({
    queryKey: ["company-picker", "search", debouncedQuery, page],
    queryFn: async () =>
      (await fetchCompanySearch({ q: debouncedQuery, page, size: PAGE_SIZE })).data,
    enabled: open && debouncedQuery.length > 0,
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  const items = searchQuery.data?.items ?? [];
  // 검색 API는 총 건수를 주지 않으므로, 가득 찬 페이지면 다음 페이지가 있다고 추정
  const totalPages = items.length === PAGE_SIZE ? page + 2 : page + 1;

  const selectedSymbols = useMemo(() => selected.map((s) => s.symbol), [selected]);

  const toggle = (symbol: string, name: string) => {
    setSelected((prev) =>
      prev.some((s) => s.symbol === symbol)
        ? prev.filter((s) => s.symbol !== symbol)
        : [...prev, { symbol, name }]
    );
  };

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="company-picker-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/40"
        aria-label="닫기"
        onClick={onClose}
      />
      <div
        ref={dialogRef}
        className="relative z-10 flex max-h-[85vh] w-full max-w-[560px] flex-col rounded-xl border border-warm-border bg-cream p-6 shadow-focus"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 id="company-picker-title" className="text-lg font-semibold text-charcoal">
              {title}
            </h2>
            {description ? (
              <p className="mt-1 text-sm text-muted-gray">{description}</p>
            ) : null}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-muted-gray hover:text-charcoal"
            aria-label="모달 닫기"
          >
            ×
          </button>
        </div>

        <div className="mt-4">
          <TextField
            label="종목 검색"
            placeholder="회사명 또는 종목코드"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                setPage(0);
              }
            }}
          />
        </div>

        {selected.length > 0 ? (
          <div className="mt-3 flex flex-wrap gap-2">
            {selected.map((s) => (
              <span
                key={s.symbol}
                className="inline-flex items-center gap-1 rounded-full border border-warm-border bg-off-white px-3 py-1 text-xs text-charcoal"
              >
                {s.name} ({s.symbol})
                <button
                  type="button"
                  onClick={() => toggle(s.symbol, s.name)}
                  className="text-muted-gray hover:text-charcoal"
                  aria-label={`${s.name} 선택 해제`}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        ) : null}

        <div className="mt-4 min-h-[240px] flex-1 overflow-y-auto rounded-lg border border-warm-border">
          {debouncedQuery.length === 0 ? (
            <EmptyState
              title="종목을 검색하세요"
              description="회사명 또는 종목코드로 검색해 추가할 종목을 선택합니다."
              showHomeLink={false}
            />
          ) : searchQuery.isLoading ? (
            <div className="space-y-2 p-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <SkeletonBlock key={i} className="h-11 w-full" />
              ))}
            </div>
          ) : searchQuery.isError ? (
            <InlineError
              message={
                searchQuery.error instanceof ApiRequestError
                  ? searchQuery.error.message
                  : "검색에 실패했습니다."
              }
              onRetry={() => void searchQuery.refetch()}
            />
          ) : items.length === 0 ? (
            <EmptyState
              title="검색 결과가 없습니다"
              description="다른 회사명 또는 종목코드로 다시 검색해 보세요."
              showHomeLink={false}
            />
          ) : (
            <ul className="divide-y divide-warm-border">
              {items.map((item) => {
                const symbol = item.ticker;
                const isSelected = symbol != null && selectedSymbols.includes(symbol);
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      disabled={symbol == null}
                      onClick={() => symbol != null && toggle(symbol, item.name)}
                      className={`flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm transition-colors hover:bg-charcoal/[0.03] disabled:cursor-not-allowed disabled:opacity-50 ${
                        isSelected ? "bg-charcoal/[0.05]" : ""
                      }`}
                    >
                      <span className="min-w-0">
                        <span className="block truncate text-charcoal">{item.name}</span>
                        <span className="block text-xs text-muted-gray">
                          {symbol ?? "코드 없음"}
                          {item.market ? ` · ${item.market}` : ""}
                        </span>
                      </span>
                      <span
                        className={`shrink-0 text-xs ${
                          isSelected ? "text-charcoal" : "text-muted-gray"
                        }`}
                      >
                        {isSelected ? "선택됨 ✓" : "추가"}
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {debouncedQuery.length > 0 && !searchQuery.isError ? (
          <Pagination
            page={page}
            totalPages={totalPages}
            onPageChange={setPage}
            disabled={searchQuery.isFetching}
          />
        ) : null}

        <div className="mt-4 flex items-center justify-end gap-3">
          <GhostButton onClick={onClose}>취소</GhostButton>
          <PrimaryButton
            className="w-auto px-6"
            disabled={selected.length === 0}
            onClick={() => onConfirm(selectedSymbols)}
          >
            {confirmLabel}
            {selected.length > 0 ? ` (${selected.length})` : ""}
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
}
