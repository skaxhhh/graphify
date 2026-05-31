import type { InputHTMLAttributes } from "react";

interface PasswordFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
}

export function PasswordField({
  label,
  error,
  hint,
  id,
  className = "",
  ...props
}: PasswordFieldProps) {
  const fieldId = id ?? label.replace(/\s+/g, "-").toLowerCase();

  return (
    <div className="scroll-mt-16 space-y-1.5">
      <label htmlFor={fieldId} className="block text-sm text-charcoal">
        {label}
      </label>
      <input
        id={fieldId}
        type="password"
        autoComplete={props.autoComplete ?? "new-password"}
        className={`h-11 w-full rounded-md border bg-cream-surface px-3 text-sm text-charcoal placeholder:text-muted-gray focus:outline-none focus:ring-2 focus:ring-ring-blue ${
          error ? "border-charcoal/60" : "border-warm-border"
        } ${className}`}
        {...props}
      />
      {hint && !error ? (
        <p className="text-xs text-muted-gray">{hint}</p>
      ) : null}
      {error ? <p className="text-xs text-charcoal">{error}</p> : null}
    </div>
  );
}
