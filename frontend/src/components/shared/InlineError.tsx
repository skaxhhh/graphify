interface InlineErrorProps {
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
}

export function InlineError({
  message,
  onRetry,
  retryLabel = "재시도",
}: InlineErrorProps) {
  return (
    <div className="px-4 py-3 text-sm text-charcoal">
      <p>{message}</p>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 underline text-muted-gray hover:text-charcoal"
        >
          {retryLabel}
        </button>
      ) : null}
    </div>
  );
}
