interface CheckboxRowProps {
  id: string;
  label: string;
  checked: boolean;
  disabled?: boolean;
  onChange: (checked: boolean) => void;
}

export function CheckboxRow({
  id,
  label,
  checked,
  disabled = false,
  onChange,
}: CheckboxRowProps) {
  return (
    <label
      htmlFor={id}
      className={`flex cursor-pointer items-start gap-3 rounded-md py-2 text-sm text-charcoal ${
        disabled ? "cursor-not-allowed opacity-60" : ""
      }`}
    >
      <input
        id={id}
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(e) => onChange(e.target.checked)}
        className="mt-0.5 h-4 w-4 rounded border-warm-border text-charcoal focus:ring-2 focus:ring-ring-blue"
      />
      <span>{label}</span>
    </label>
  );
}
