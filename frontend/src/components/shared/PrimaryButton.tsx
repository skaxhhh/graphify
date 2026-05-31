import type { ButtonHTMLAttributes } from "react";

interface PrimaryButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
}

export function PrimaryButton({
  loading = false,
  disabled,
  children,
  className = "",
  type = "button",
  ...props
}: PrimaryButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={`flex h-11 w-full items-center justify-center rounded-md bg-charcoal px-4 text-sm text-off-white shadow-btn-inset transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-80 ${className}`}
      {...props}
    >
      {loading ? (
        <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-off-white/30 border-t-off-white" />
      ) : (
        children
      )}
    </button>
  );
}
