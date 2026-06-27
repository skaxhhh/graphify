// Replaces EmptyState tone="dark" / ErrorBanner tone="dark" / SkeletonBlock tone="dark"
// Uses trade tokens instead of cream-system gray palette.
// CRITICAL: message/title strings are passed by consumers — render as-is so existing
// e2e text assertions stay green.
type TradePageStateVariant = "loading" | "empty" | "error";

interface TradePageStateProps {
  variant: TradePageStateVariant;
  title?: string;
  message?: string;
  onRetry?: () => void;
  className?: string;
}

export function TradePageState({
  variant,
  title,
  message,
  onRetry,
  className = "",
}: TradePageStateProps) {
  if (variant === "loading") {
    return (
      <div
        className={`animate-pulse rounded-lg bg-trade-surface border border-trade-hairline ${className}`}
        style={{ minHeight: "200px" }}
      />
    );
  }

  return (
    <div
      className={`flex min-h-[200px] flex-col items-center justify-center rounded-lg border border-trade-hairline bg-trade-surface px-6 py-10 text-center ${className}`}
    >
      {variant === "error" && (
        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-trade-down-soft">
          <span className="text-trade-down font-bold text-lg">!</span>
        </div>
      )}
      {title && (
        <p className="text-sm font-medium text-trade-body font-trade-sans">
          {title}
        </p>
      )}
      {message && (
        <p className="mt-1 text-xs text-trade-muted font-trade-sans">{message}</p>
      )}
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-4 text-xs text-trade-primary hover:text-trade-primary-active underline font-trade-sans"
        >
          재시도
        </button>
      )}
    </div>
  );
}
