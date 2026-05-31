import type { ButtonHTMLAttributes } from "react";

interface GhostButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  fullWidth?: boolean;
}

export function GhostButton({
  fullWidth = false,
  className = "",
  children,
  ...props
}: GhostButtonProps) {
  return (
    <button
      type="button"
      className={`rounded-md border border-charcoal/40 bg-transparent px-4 py-3 text-sm text-charcoal transition-opacity hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50 ${
        fullWidth ? "w-full" : ""
      } ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
