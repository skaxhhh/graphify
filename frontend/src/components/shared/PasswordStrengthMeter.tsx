interface PasswordStrengthMeterProps {
  strength: 0 | 1 | 2 | 3 | 4;
}

const labels = ["", "약함", "보통", "좋음", "강함"] as const;

export function PasswordStrengthMeter({ strength }: PasswordStrengthMeterProps) {
  if (strength === 0) {
    return null;
  }

  return (
    <div className="space-y-1.5" aria-hidden>
      <div className="flex gap-1">
        {[1, 2, 3, 4].map((level) => (
          <div
            key={level}
            className={`h-1 flex-1 rounded-full transition-colors ${
              strength >= level ? "bg-charcoal" : "bg-light-cream"
            }`}
          />
        ))}
      </div>
      <p className="text-xs text-muted-gray">{labels[strength]}</p>
    </div>
  );
}
