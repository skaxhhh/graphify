import type { UserUsageRow } from "@/types/admin";

interface SessionDetailDrawerProps {
  open: boolean;
  row: UserUsageRow | null;
  onClose: () => void;
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat("ko-KR").format(value);
}

export function SessionDetailDrawer({
  open,
  row,
  onClose,
}: SessionDetailDrawerProps) {
  if (!open || !row) return null;

  return (
    <>
      <button
        type="button"
        className="fixed inset-0 z-40 bg-charcoal/30"
        aria-label="닫기"
        onClick={onClose}
      />
      <aside
        className="fixed inset-y-0 right-0 z-50 w-full max-w-md border-l border-warm-border bg-cream p-6 shadow-xl transition-transform duration-300 ease-out"
        role="dialog"
        aria-modal="true"
        aria-labelledby="session-drawer-title"
      >
        <div className="flex items-start justify-between gap-4">
          <h2 id="session-drawer-title" className="text-lg font-semibold text-charcoal">
            사용자 사용 상세
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md px-2 py-1 text-sm text-muted-gray hover:bg-light-cream hover:text-charcoal"
          >
            닫기
          </button>
        </div>
        <dl className="mt-6 space-y-4 text-sm">
          <div>
            <dt className="text-muted-gray">이름</dt>
            <dd className="font-medium text-charcoal">{row.name}</dd>
          </div>
          <div>
            <dt className="text-muted-gray">이메일</dt>
            <dd className="text-charcoal">{row.email}</dd>
          </div>
          <div>
            <dt className="text-muted-gray">분석 요청</dt>
            <dd className="text-charcoal">{formatNumber(row.requests)}건</dd>
          </div>
          <div>
            <dt className="text-muted-gray">토큰 사용</dt>
            <dd className="text-charcoal">{formatNumber(row.tokens)}</dd>
          </div>
          <div>
            <dt className="text-muted-gray">오류</dt>
            <dd className="text-charcoal">{formatNumber(row.errors)}건</dd>
          </div>
        </dl>
        <p className="mt-8 text-xs text-muted-gray">
          세션 단위 로그 API는 후속 태스크에서 연동됩니다.
        </p>
      </aside>
    </>
  );
}
