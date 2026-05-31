import type { AdminAlert } from "@/types/admin";

function severityClass(severity: string): string {
  const normalized = severity.toUpperCase();
  if (normalized === "WARN" || normalized === "WARNING") {
    return "border-amber-200 bg-amber-50 text-amber-900";
  }
  if (normalized === "ERROR" || normalized === "CRITICAL") {
    return "border-red-200 bg-red-50 text-red-900";
  }
  return "border-warm-border bg-light-cream/60 text-charcoal";
}

function formatDetectedAt(value: string): string {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface AnomalyAlertListProps {
  alerts: AdminAlert[];
}

export function AnomalyAlertList({ alerts }: AnomalyAlertListProps) {
  return (
    <section className="flex min-h-[12rem] flex-col rounded-xl border border-warm-border bg-cream p-4">
      <h2 className="text-base font-semibold text-charcoal">이상 알림</h2>
      {alerts.length === 0 ? (
        <p className="mt-6 text-sm text-muted-gray">
          현재 감지된 이상 징후가 없습니다.
        </p>
      ) : (
        <ul className="mt-3 max-h-64 space-y-2 overflow-y-auto">
          {alerts.map((alert, index) => (
            <li
              key={`${alert.detectedAt}-${index}`}
              className={`rounded-lg border px-3 py-2 text-sm ${severityClass(alert.severity)}`}
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-medium">{alert.severity}</span>
                <time className="text-xs opacity-80">
                  {formatDetectedAt(alert.detectedAt)}
                </time>
              </div>
              <p className="mt-1">{alert.message}</p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
