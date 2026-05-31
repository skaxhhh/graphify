import { useEffect } from "react";
import type { InsightCard } from "@/types/company";

interface InsightEvidenceModalProps {
  card: InsightCard | null;
  updatedAt?: string;
  onClose: () => void;
}

export function InsightEvidenceModal({ card, updatedAt, onClose }: InsightEvidenceModalProps) {
  useEffect(() => {
    if (!card) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [card, onClose]);

  if (!card) return null;

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="insight-evidence-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/[0.03] transition-opacity duration-200 ease-out"
        aria-label="닫기"
        onClick={onClose}
      />
      <div className="relative z-10 w-full max-w-[560px] scale-100 rounded-xl border border-warm-border bg-cream p-6 shadow-focus transition duration-200 ease-out">
        <div className="flex items-start justify-between gap-4">
          <h2 id="insight-evidence-title" className="text-lg font-semibold text-charcoal">
            {card.title}
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
        <p className="mt-4 text-sm leading-relaxed text-charcoal">
          {card.evidence ?? "근거 상세를 불러올 수 없습니다."}
        </p>
        <p className="mt-4 text-xs text-muted-gray">
          MCP 추적: disclosure-api, news-api · 규칙: 공시·뉴스 교차 검증
          {updatedAt ? ` · 갱신 ${new Date(updatedAt).toLocaleString("ko-KR")}` : ""}
        </p>
      </div>
    </div>
  );
}
