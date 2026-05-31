interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export function Pagination({
  page,
  totalPages,
  onPageChange,
  disabled = false,
}: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav
      className="flex items-center justify-center gap-2 py-6"
      aria-label="검색 결과 페이지"
    >
      <button
        type="button"
        disabled={disabled || page <= 0}
        onClick={() => onPageChange(page - 1)}
        className="rounded-md border border-warm-border px-3 py-1.5 text-sm text-charcoal disabled:opacity-40"
      >
        이전
      </button>
      <span className="text-sm text-muted-gray">
        {page + 1} / {totalPages}
      </span>
      <button
        type="button"
        disabled={disabled || page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className="rounded-md border border-warm-border px-3 py-1.5 text-sm text-charcoal disabled:opacity-40"
      >
        다음
      </button>
    </nav>
  );
}
