import type { InputHTMLAttributes } from "react";

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export function TextField({
  label,
  error,
  id,
  className = "",
  ...props
}: TextFieldProps) {
  const fieldId = id ?? label.replace(/\s+/g, "-").toLowerCase();

  return (
    <div className="space-y-1.5">
      <label htmlFor={fieldId} className="block text-sm text-charcoal">
        {label}
      </label>
      <input
        id={fieldId}
        className={`h-11 w-full rounded-md border bg-cream px-3 text-sm text-charcoal placeholder:text-muted-gray focus:outline-none focus:ring-2 focus:ring-ring-blue ${
          error ? "border-charcoal/60" : "border-warm-border"
        } ${className}`}
        {...props}
      />
      {error ? <p className="text-xs text-charcoal">{error}</p> : null}
    </div>
  );
}
