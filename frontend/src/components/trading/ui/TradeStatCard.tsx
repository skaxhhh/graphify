// Source: DESIGN-binance.md stat-callout-card + trade tokens
interface TradeStatCardProps {
  label: string;
  value: string | number;
  sub?: string;
  valueColor?: "up" | "down" | "neutral";
  className?: string;
}

export function TradeStatCard({
  label,
  value,
  sub,
  valueColor = "neutral",
  className = "",
}: TradeStatCardProps) {
  const colorMap: Record<string, string> = {
    up: "text-trade-up",
    down: "text-trade-down",
    neutral: "text-trade-on-dark",
  };

  return (
    <div
      className={`rounded-lg bg-trade-surface border border-trade-hairline p-4 ${className}`}
    >
      <p className="text-xs text-trade-muted font-trade-sans">{label}</p>
      <p className={`mt-1 text-xl font-bold font-trade-mono ${colorMap[valueColor]}`}>
        {value}
      </p>
      {sub && <p className="mt-0.5 text-xs text-trade-muted font-trade-sans">{sub}</p>}
    </div>
  );
}
