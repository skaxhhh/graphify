interface CompareToggleProps {
  enabled: boolean;
  onChange: (enabled: boolean) => void;
}

export function CompareToggle({ enabled, onChange }: CompareToggleProps) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2 text-sm text-charcoal">
      <span className="text-muted-gray">현재와 비교</span>
      <button
        type="button"
        role="switch"
        aria-checked={enabled}
        onClick={() => onChange(!enabled)}
        className={`relative h-6 w-11 rounded-full transition-colors duration-300 ${
          enabled ? "bg-charcoal" : "bg-light-cream"
        }`}
      >
        <span
          className={`absolute top-0.5 left-0.5 h-5 w-5 rounded-full bg-cream shadow transition-transform duration-300 ease-out ${
            enabled ? "translate-x-5" : "translate-x-0"
          }`}
        />
      </button>
    </label>
  );
}
