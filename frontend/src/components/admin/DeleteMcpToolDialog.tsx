import { useEffect } from "react";
import { GhostButton } from "@/components/shared/GhostButton";
import { PrimaryButton } from "@/components/shared/PrimaryButton";

interface DeleteMcpToolDialogProps {
  open: boolean;
  toolName: string;
  loading?: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export function DeleteMcpToolDialog({
  open,
  toolName,
  loading = false,
  onClose,
  onConfirm,
}: DeleteMcpToolDialogProps) {
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
      aria-labelledby="delete-mcp-tool-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/20"
        aria-label="닫기"
        onClick={onClose}
      />
      <div className="relative z-10 w-full max-w-[400px] rounded-xl border border-warm-border bg-cream p-6 shadow-focus">
        <h2 id="delete-mcp-tool-title" className="text-lg font-semibold text-charcoal">
          MCP 도구 삭제
        </h2>
        <p className="mt-2 text-sm text-muted-gray">
          <strong className="text-charcoal">{toolName}</strong> 도구를 삭제할까요? 이 작업은
          되돌릴 수 없습니다.
        </p>
        <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:justify-end">
          <GhostButton type="button" onClick={onClose} disabled={loading}>
            취소
          </GhostButton>
          <PrimaryButton
            type="button"
            className="!w-auto sm:min-w-[120px] !bg-red-700 hover:!bg-red-800"
            loading={loading}
            onClick={onConfirm}
          >
            삭제
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
}
