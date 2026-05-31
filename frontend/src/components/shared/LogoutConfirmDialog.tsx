import { useEffect } from "react";
import { GhostButton } from "@/components/shared/GhostButton";
import { PrimaryButton } from "@/components/shared/PrimaryButton";

interface LogoutConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  loading?: boolean;
}

export function LogoutConfirmDialog({
  open,
  onClose,
  onConfirm,
  loading = false,
}: LogoutConfirmDialogProps) {
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
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-[fadeIn_150ms_ease-out]"
      role="dialog"
      aria-modal="true"
      aria-labelledby="logout-dialog-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/20"
        aria-label="닫기"
        onClick={onClose}
      />
      <div className="relative z-10 w-full max-w-[400px] rounded-xl border border-warm-border bg-cream p-6 shadow-focus">
        <h2 id="logout-dialog-title" className="text-lg font-semibold text-charcoal">
          로그아웃
        </h2>
        <p className="mt-2 text-sm text-muted-gray">로그아웃 하시겠습니까?</p>
        <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:justify-end">
          <GhostButton type="button" onClick={onClose} disabled={loading}>
            취소
          </GhostButton>
          <PrimaryButton
            type="button"
            className="!w-auto sm:min-w-[120px]"
            loading={loading}
            onClick={onConfirm}
          >
            로그아웃
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
}
