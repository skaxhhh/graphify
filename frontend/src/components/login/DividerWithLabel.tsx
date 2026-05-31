interface DividerWithLabelProps {
  label?: string;
}

export function DividerWithLabel({ label = "또는" }: DividerWithLabelProps) {
  return (
    <div className="relative my-8 border-t border-warm-border pt-8">
      <span className="absolute left-1/2 top-0 -translate-x-1/2 -translate-y-1/2 bg-cream px-3 text-xs text-muted-gray">
        {label}
      </span>
    </div>
  );
}
