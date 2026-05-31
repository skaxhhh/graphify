import { useEffect } from "react";

interface ReliabilityCriteriaModalProps {
  open: boolean;
  onClose: () => void;
}

export function ReliabilityCriteriaModal({ open, onClose }: ReliabilityCriteriaModalProps) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="reliability-modal-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/[0.03]"
        aria-label="닫기"
        onClick={onClose}
      />
      <div className="relative z-10 w-full max-w-[480px] rounded-xl border border-warm-border bg-cream p-6 shadow-focus">
        <h2 id="reliability-modal-title" className="text-lg font-semibold text-charcoal">
          신뢰도 기준 (OV06)
        </h2>
        <ul className="mt-4 list-disc space-y-2 pl-5 text-sm text-muted-gray">
          <li>
            <strong className="text-charcoal">높음</strong>: 공시·IR·복수 MCP 출처 일치
          </li>
          <li>
            <strong className="text-charcoal">보통</strong>: 단일 공시 또는 업계 리포트 1건 이상
          </li>
          <li>
            <strong className="text-charcoal">낮음</strong>: 추론·간접 관계만 확인된 경우
          </li>
        </ul>
        <button
          type="button"
          onClick={onClose}
          className="mt-6 text-sm text-muted-gray underline hover:text-charcoal"
        >
          닫기
        </button>
      </div>
    </div>
  );
}
