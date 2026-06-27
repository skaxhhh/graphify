// Thin layout-agnostic table wrapper. Consumers pass grid-template via className
// so existing table grids drop in unchanged.
interface TradeTableProps {
  className?: string;
  children: React.ReactNode;
}

export function TradeTable({ className = "", children }: TradeTableProps) {
  return (
    <div
      className={`rounded-lg bg-trade-surface border border-trade-hairline overflow-hidden ${className}`}
    >
      {children}
    </div>
  );
}

interface TradeTableHeaderProps {
  className?: string;
  children: React.ReactNode;
}

export function TradeTableHeader({
  className = "",
  children,
}: TradeTableHeaderProps) {
  return (
    <div
      className={`border-b border-trade-hairline px-4 py-2.5 text-xs text-trade-muted font-trade-sans ${className}`}
    >
      {children}
    </div>
  );
}

interface TradeTableRowProps {
  className?: string;
  onClick?: () => void;
  children: React.ReactNode;
}

export function TradeTableRow({
  className = "",
  onClick,
  children,
}: TradeTableRowProps) {
  return (
    <div
      onClick={onClick}
      className={`border-b border-trade-hairline last:border-b-0 px-4 py-3 hover:bg-trade-elevated transition-colors ${onClick ? "cursor-pointer" : ""} ${className}`}
    >
      {children}
    </div>
  );
}
