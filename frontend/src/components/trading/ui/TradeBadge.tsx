// Source: DESIGN-binance.md trust-badge + trading semantics
type TradeBadgeVariant =
  | "active"
  | "draft"
  | "running"
  | "stopped"
  | "up"
  | "down"
  | "info"
  | "warning";

const badgeStyles: Record<TradeBadgeVariant, string> = {
  active: "bg-trade-up-soft text-trade-up border border-trade-up/30",
  draft: "bg-trade-elevated text-trade-muted border border-trade-hairline",
  running: "bg-trade-up-soft text-trade-up border border-trade-up/30",
  stopped:
    "bg-trade-elevated text-trade-muted-strong border border-trade-hairline",
  up: "bg-trade-up-soft text-trade-up",
  down: "bg-trade-down-soft text-trade-down",
  info: "bg-trade-info/10 text-trade-info",
  warning: "bg-trade-primary/10 text-trade-primary",
};

interface TradeBadgeProps {
  variant: TradeBadgeVariant;
  className?: string;
  children: React.ReactNode;
}

export function TradeBadge({
  variant,
  className = "",
  children,
}: TradeBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded px-2 py-0.5 text-xs font-semibold font-trade-sans ${badgeStyles[variant]} ${className}`}
    >
      {children}
    </span>
  );
}
