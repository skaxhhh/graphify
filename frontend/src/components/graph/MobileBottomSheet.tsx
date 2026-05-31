import type { ReactNode } from "react";

interface MobileBottomSheetProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}

export function MobileBottomSheet({ open, title, onClose, children }: MobileBottomSheetProps) {
  if (!open) return null;

  return (
    <>
      <button
        type="button"
        className="fixed inset-0 z-40 bg-charcoal/20 lg:hidden"
        aria-label="닫기"
        onClick={onClose}
      />
      <section className="fixed inset-x-0 bottom-0 z-50 max-h-[70vh] overflow-y-auto rounded-t-xl border border-warm-border bg-cream p-4 lg:hidden">
        <header className="mb-4 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-charcoal">{title}</h3>
          <button type="button" onClick={onClose} className="text-muted-gray">
            닫기
          </button>
        </header>
        {children}
      </section>
    </>
  );
}
