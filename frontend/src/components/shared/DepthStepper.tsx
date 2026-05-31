interface DepthStepperProps {
  value: number;
  onChange: (depth: number) => void;
}

const DEPTHS = [1, 2, 3] as const;

export function DepthStepper({ value, onChange }: DepthStepperProps) {
  return (
    <div className="flex items-center gap-1 rounded-md border border-warm-border bg-cream p-1">
      <span className="px-2 text-xs text-muted-gray">깊이</span>
      {DEPTHS.map((depth) => (
        <button
          key={depth}
          type="button"
          onClick={() => onChange(depth)}
          className={`min-w-[2rem] rounded px-2 py-1 text-xs font-medium transition-colors ${
            value === depth
              ? "bg-charcoal text-off-white"
              : "text-charcoal hover:bg-charcoal/[0.06]"
          }`}
          aria-pressed={value === depth}
        >
          {depth}
        </button>
      ))}
    </div>
  );
}
