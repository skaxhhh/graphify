import { ErrorBanner } from "@/components/shared/ErrorBanner";

interface JobProgressBannerProps {
  status: string;
  progress: number;
  message: string | null;
  error?: string | null;
}

export function JobProgressBanner({
  status,
  progress,
  message,
  error,
}: JobProgressBannerProps) {
  if (error) {
    return <ErrorBanner message={error} />;
  }

  if (status !== "RUNNING" && status !== "PENDING") {
    return null;
  }

  return (
    <div
      className="rounded-lg border border-warm-border bg-cream px-4 py-3"
      role="status"
      aria-live="polite"
    >
      <div className="flex items-center justify-between gap-4 text-sm">
        <span className="text-muted-gray">{message ?? "작업 진행 중"}</span>
        <span className="font-medium text-charcoal">{progress}%</span>
      </div>
      <div
        className="mt-2 h-1.5 overflow-hidden rounded-full bg-light-cream"
        aria-hidden
      >
        <div
          className="h-full rounded-full bg-charcoal transition-all duration-300"
          style={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
        />
      </div>
    </div>
  );
}
