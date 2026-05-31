import { GhostButton } from "@/components/shared/GhostButton";
import { PrimaryButton } from "@/components/shared/PrimaryButton";

interface RollbackConfirmDialogProps {
  open: boolean;
  versionLabel: string;
  loading: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export function RollbackConfirmDialog({
  open,
  versionLabel,
  loading,
  onClose,
  onConfirm,
}: RollbackConfirmDialogProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/30"
        aria-label="닫기"
        onClick={onClose}
      />
      <div
        className="relative w-full max-w-md rounded-lg border border-warm-border bg-cream p-6 shadow-lg"
        role="alertdialog"
        aria-labelledby="rollback-title"
      >
        <h3 id="rollback-title" className="text-lg font-semibold text-charcoal">
          버전 롤백
        </h3>
        <p className="mt-2 text-sm text-muted-gray">
          {versionLabel} 내용으로 에디터를 되돌립니다. 롤백도 새 버전으로 기록됩니다.
        </p>
        <div className="mt-6 flex justify-end gap-2">
          <GhostButton type="button" onClick={onClose} disabled={loading}>
            취소
          </GhostButton>
          <PrimaryButton
            type="button"
            className="!w-auto"
            loading={loading}
            onClick={onConfirm}
          >
            롤백
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
}
