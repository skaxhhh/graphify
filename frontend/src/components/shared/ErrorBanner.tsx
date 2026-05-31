interface ErrorBannerProps {
  message: string;
  onRetry?: () => void;
}

export function ErrorBanner({ message, onRetry }: ErrorBannerProps) {
  return (
    <div
      role="alert"
      className="mb-4 rounded-lg border border-warm-border bg-cream px-4 py-3 text-sm text-charcoal"
    >
      <p>{message}</p>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 text-sm underline text-muted-gray hover:text-charcoal"
        >
          다시 시도
        </button>
      ) : null}
    </div>
  );
}
