// TradingCompanyPickerModal — trade-themed stock picker (06.8-02)
// Props interface is identical to shared/CompanyPickerModal so consumers swap import path only.
// Uses trading/ui primitives; shared/CompanyPickerModal.tsx is NOT modified (D6).
import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchCompanySearch } from "@/lib/searchApi";
import { ApiRequestError } from "@/lib/apiClient";
import { useDebounce } from "@/hooks/useDebounce";
import { TradeButton, TradeInput, TradePageState } from "@/components/trading/ui";

const PAGE_SIZE = 10;

interface SelectedCompany {
  symbol: string;
  name: string;
}

// Exact same props as shared/CompanyPickerModal
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

export function TradingCompanyPickerModal({
  open,
  title = "종목 직접 선택",
  description,
  confirmLabel = "선택 적용",
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
      aria-labelledby="trade-company-picker-title"
    >
      {/* Overlay — bg-black/60 per wireframe */}
      <button
        type="button"
        className="absolute inset-0 bg-black/60"
        aria-label="닫기"
        onClick={onClose}
      />

      {/* Panel — bg-trade-surface border-trade-hairline rounded-xl (wireframe lines ~383–397) */}
      <div
        ref={dialogRef}
        className="relative z-10 flex max-h-[85vh] w-full max-w-[560px] flex-col rounded-xl border border-trade-hairline bg-trade-surface p-6 shadow-focus"
      >
        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="trade-company-picker-title"
              className="text-lg font-semibold text-trade-on-dark"
            >
              {title}
            </h2>
            {description ? (
              <p className="mt-1 text-sm text-trade-muted">{description}</p>
            ) : null}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-trade-muted hover:text-trade-body"
            aria-label="모달 닫기"
          >
            ×
          </button>
        </div>

        {/* Search — TradeInput (bg-trade-surface border-trade-hairline) */}
        <div className="mt-4">
          <TradeInput
            placeholder="🔍 종목명 또는 코드 검색…"
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

        {/* Selected chips — bg-trade-primary text-trade-ink rounded-full (wireframe line ~388) */}
        {selected.length > 0 ? (
          <div className="mt-3 flex flex-wrap gap-2">
            {selected.map((s) => (
              <span
                key={s.symbol}
                className="inline-flex items-center gap-1 rounded-full bg-trade-primary px-3 py-1 text-xs font-semibold text-trade-ink"
              >
                {s.name}
                <button
                  type="button"
                  onClick={() => toggle(s.symbol, s.name)}
                  className="ml-0.5 text-trade-ink/70 hover:text-trade-ink"
                  aria-label={`${s.name} 선택 해제`}
                >
                  ✕
                </button>
              </span>
            ))}
          </div>
        ) : null}

        {/* Results list */}
        <div className="mt-4 min-h-[240px] flex-1 overflow-y-auto rounded-lg border border-trade-hairline bg-trade-bg">
          {debouncedQuery.length === 0 ? (
            <TradePageState
              variant="empty"
              title="종목을 검색하세요"
              message="종목명 또는 코드로 검색해 추가할 종목을 선택합니다."
            />
          ) : searchQuery.isLoading ? (
            <TradePageState variant="loading" />
          ) : searchQuery.isError ? (
            <TradePageState
              variant="error"
              title="검색 실패"
              message={
                searchQuery.error instanceof ApiRequestError
                  ? searchQuery.error.message
                  : "검색에 실패했습니다."
              }
              onRetry={() => void searchQuery.refetch()}
            />
          ) : items.length === 0 ? (
            <TradePageState
              variant="empty"
              title="검색 결과 없음"
              message="다른 종목명 또는 코드로 다시 검색해 보세요."
            />
          ) : (
            <ul className="divide-y divide-trade-hairline">
              {items.map((item) => {
                const symbol = item.ticker;
                const isSelected = symbol != null && selectedSymbols.includes(symbol);
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      disabled={symbol == null}
                      onClick={() => symbol != null && toggle(symbol, item.name)}
                      className={`flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm transition-colors hover:bg-trade-elevated disabled:cursor-not-allowed disabled:opacity-50 ${
                        isSelected ? "bg-trade-elevated" : ""
                      }`}
                    >
                      <span className="min-w-0">
                        <span className="block truncate text-trade-body">{item.name}</span>
                        <span className="block text-xs text-trade-muted">
                          {symbol ?? "코드 없음"}
                          {item.market ? ` · ${item.market}` : ""}
                        </span>
                      </span>
                      {/* Candidate chip — border-trade-hairline text-trade-muted-strong (wireframe line ~390) */}
                      <span
                        className={`shrink-0 rounded-full border px-2.5 py-0.5 text-xs font-medium ${
                          isSelected
                            ? "border-trade-primary/40 bg-trade-primary/10 text-trade-primary"
                            : "border-trade-hairline text-trade-muted-strong"
                        }`}
                      >
                        {isSelected ? "선택됨 ✓" : "+ 추가"}
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {/* Pagination — inline trade-token buttons */}
        {debouncedQuery.length > 0 && !searchQuery.isError && totalPages > 1 ? (
          <div className="mt-3 flex items-center justify-center gap-3">
            <button
              type="button"
              disabled={page === 0 || searchQuery.isFetching}
              onClick={() => setPage((p) => p - 1)}
              className="rounded border border-trade-hairline px-3 py-1 text-sm text-trade-muted-strong hover:bg-trade-elevated disabled:opacity-40"
            >
              이전
            </button>
            <span className="font-trade-mono text-xs text-trade-muted">
              {page + 1} / {totalPages}
            </span>
            <button
              type="button"
              disabled={page + 1 >= totalPages || searchQuery.isFetching}
              onClick={() => setPage((p) => p + 1)}
              className="rounded border border-trade-hairline px-3 py-1 text-sm text-trade-muted-strong hover:bg-trade-elevated disabled:opacity-40"
            >
              다음
            </button>
          </div>
        ) : null}

        {/* Footer — "취소" TradeButton secondary, "선택 적용" TradeButton primary */}
        <div className="mt-4 flex items-center justify-end gap-3">
          <TradeButton variant="secondary" onClick={onClose}>
            취소
          </TradeButton>
          <TradeButton
            variant="primary"
            disabled={selected.length === 0}
            onClick={() => onConfirm(selectedSymbols)}
          >
            {confirmLabel}
            {selected.length > 0 ? ` (${selected.length})` : ""}
          </TradeButton>
        </div>
      </div>
    </div>
  );
}
