interface TradeCardProps {
  title?: string;
  className?: string;
  children: React.ReactNode;
}

export function TradeCard({ title, className = "", children }: TradeCardProps) {
  return (
    <div
      className={`rounded-lg bg-trade-surface border border-trade-hairline p-4 ${className}`}
    >
      {title && (
        <p className="text-sm font-medium text-trade-body font-trade-sans mb-3">
          {title}
        </p>
      )}
      {children}
    </div>
  );
}
