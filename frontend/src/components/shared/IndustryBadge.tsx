interface IndustryBadgeProps {
  industry: string | null;
}

export function IndustryBadge({ industry }: IndustryBadgeProps) {
  if (!industry) return null;
  return (
    <span className="inline-flex items-center rounded-md border border-warm-border bg-charcoal/[0.03] px-2.5 py-1 text-xs font-medium text-muted-gray">
      {industry}
    </span>
  );
}
