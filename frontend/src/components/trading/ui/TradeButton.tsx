// Source: DESIGN-binance.md button-primary / button-secondary-on-dark / button-primary-disabled
interface TradeButtonProps {
  variant?: "primary" | "secondary" | "danger" | "ghost";
  size?: "sm" | "md";
  disabled?: boolean;
  loading?: boolean;
  type?: "button" | "submit" | "reset";
  onClick?: () => void;
  className?: string;
  children: React.ReactNode;
}

export function TradeButton({
  variant = "primary",
  size = "md",
  disabled,
  loading,
  type = "button",
  onClick,
  className = "",
  children,
}: TradeButtonProps) {
  const base =
    "inline-flex items-center justify-center rounded font-trade-sans font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-trade-info disabled:cursor-not-allowed";
  const sizes: Record<string, string> = {
    sm: "h-7 px-3 text-xs",
    md: "h-10 px-5 text-sm",
  };
  const variants: Record<string, string> = {
    primary:
      "bg-trade-primary text-trade-ink hover:bg-trade-primary-active disabled:bg-trade-primary-disabled disabled:text-trade-muted",
    secondary:
      "bg-trade-surface text-trade-body border border-trade-hairline hover:bg-trade-elevated",
    danger: "bg-trade-down text-trade-on-dark hover:opacity-90",
    ghost: "bg-transparent text-trade-body hover:bg-trade-elevated",
  };

  return (
    <button
      type={type}
      disabled={disabled || loading}
      onClick={onClick}
      className={`${base} ${sizes[size]} ${variants[variant]} ${className}`}
    >
      {loading ? "..." : children}
    </button>
  );
}
