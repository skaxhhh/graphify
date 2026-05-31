import { useEffect } from "react";
import type { TermItem } from "@/types/search";

interface TermsDetailModalProps {
  term: TermItem | null;
  onClose: () => void;
}

export function TermsDetailModal({ term, onClose }: TermsDetailModalProps) {
  useEffect(() => {
    if (!term) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [term, onClose]);

  if (!term) return null;

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="terms-modal-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/[0.03] transition-opacity duration-200 ease-out"
        aria-label="닫기"
        onClick={onClose}
      />
      <div className="relative z-10 flex max-h-[min(80vh,640px)] w-full max-w-[560px] flex-col rounded-xl border border-warm-border bg-cream shadow-focus">
        <div className="flex items-center justify-between border-b border-warm-border px-6 py-4">
          <h2 id="terms-modal-title" className="text-lg font-semibold text-charcoal">
            {term.title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-muted-gray hover:text-charcoal"
            aria-label="모달 닫기"
          >
            ×
          </button>
        </div>
        <div className="overflow-y-auto px-6 py-4 text-sm leading-relaxed text-charcoal">
          {term.content ?? "약관 본문을 불러올 수 없습니다."}
        </div>
      </div>
    </div>
  );
}
