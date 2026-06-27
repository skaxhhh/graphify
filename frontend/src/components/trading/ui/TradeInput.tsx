import { forwardRef } from "react";

// Source: DESIGN-binance.md search-input-on-dark
interface TradeInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  mono?: boolean; // set true for numeric/price fields → font-trade-mono
  className?: string;
}

export const TradeInput = forwardRef<HTMLInputElement, TradeInputProps>(
  ({ mono = false, className = "", ...props }, ref) => {
    return (
      <input
        ref={ref}
        {...props}
        className={`bg-trade-surface text-trade-body border border-trade-hairline rounded-md px-3 h-10 w-full placeholder:text-trade-muted focus:ring-2 focus:ring-trade-info focus:outline-none transition-colors ${
          mono ? "font-trade-mono" : "font-trade-sans"
        } ${className}`}
      />
    );
  }
);

TradeInput.displayName = "TradeInput";
