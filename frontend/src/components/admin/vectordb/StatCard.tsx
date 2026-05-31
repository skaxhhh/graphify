interface StatCardProps {
  label: string;
  value: string;
  hint?: string;
}

export function StatCard({ label, value, hint }: StatCardProps) {
  return (
    <article className="flex min-h-[100px] flex-col justify-between rounded-xl border border-warm-border bg-cream p-4 shadow-sm">
      <p className="text-sm text-muted-gray">{label}</p>
      <p className="text-2xl font-semibold text-charcoal">{value}</p>
      {hint ? (
        <p className="text-xs text-muted-gray">{hint}</p>
      ) : (
        <span className="h-4" aria-hidden />
      )}
    </article>
  );
}
