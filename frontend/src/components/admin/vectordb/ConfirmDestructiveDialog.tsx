import { GhostButton } from "@/components/shared/GhostButton";
import { TextField } from "@/components/shared/TextField";

interface ConfirmDestructiveDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  requirePhrase?: string;
  confirmPhrase: string;
  onConfirmPhraseChange: (value: string) => void;
  loading?: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export function ConfirmDestructiveDialog({
  open,
  title,
  description,
  confirmLabel = "삭제 실행",
  requirePhrase,
  confirmPhrase,
  onConfirmPhraseChange,
  loading = false,
  onClose,
  onConfirm,
}: ConfirmDestructiveDialogProps) {
  if (!open) return null;

  const phraseOk = requirePhrase
    ? confirmPhrase.trim() === requirePhrase
    : true;

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-[fadeIn_150ms_ease-out]"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="destructive-dialog-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/20"
        aria-label="닫기"
        onClick={onClose}
      />
      <div className="relative z-10 w-full max-w-md rounded-xl border border-charcoal/40 bg-cream p-6 shadow-focus">
        <h2 id="destructive-dialog-title" className="text-lg font-semibold text-charcoal">
          {title}
        </h2>
        <p className="mt-2 text-sm text-muted-gray">{description}</p>
        {requirePhrase ? (
          <div className="mt-4">
            <p className="mb-2 text-xs text-muted-gray">
              계속하려면 <strong className="text-charcoal">{requirePhrase}</strong> 를 입력하세요.
            </p>
            <TextField
              id="cleanup-confirm-phrase"
              label="확인 문구"
              value={confirmPhrase}
              onChange={(e) => onConfirmPhraseChange(e.target.value)}
              autoComplete="off"
            />
          </div>
        ) : null}
        <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:justify-end">
          <GhostButton type="button" onClick={onClose} disabled={loading}>
            취소
          </GhostButton>
          <button
            type="button"
            disabled={loading || !phraseOk}
            onClick={onConfirm}
            className="inline-flex min-h-[44px] items-center justify-center rounded-lg border border-charcoal/40 bg-transparent px-5 text-sm font-medium text-charcoal transition hover:bg-light-cream disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? "처리 중…" : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
