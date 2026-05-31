interface SuccessNoticeProps {
  message: string;
  maskedEmail: string;
}

export function SuccessNotice({ message, maskedEmail }: SuccessNoticeProps) {
  return (
    <div
      role="status"
      className="animate-[fadeIn_0.2s_ease-out] rounded-md border border-warm-border bg-cream p-4"
    >
      <p className="text-sm text-charcoal">{message}</p>
      <p className="mt-2 text-sm text-muted-gray">
        발송 주소: <span className="text-charcoal">{maskedEmail}</span>
      </p>
    </div>
  );
}
