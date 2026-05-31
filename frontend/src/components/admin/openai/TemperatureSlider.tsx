interface TemperatureSliderProps {
  value: number;
  onChange: (value: number) => void;
  disabled?: boolean;
}

export function TemperatureSlider({
  value,
  onChange,
  disabled = false,
}: TemperatureSliderProps) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-sm text-charcoal">
        <span>Temperature</span>
        <span className="font-mono text-muted-gray">{value.toFixed(2)}</span>
      </div>
      <input
        type="range"
        min={0}
        max={2}
        step={0.01}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(Number(e.target.value))}
        className="h-10 w-full cursor-pointer appearance-none rounded-full bg-light-cream disabled:opacity-50 [&::-moz-range-thumb]:h-5 [&::-moz-range-thumb]:w-5 [&::-moz-range-thumb]:rounded-full [&::-moz-range-thumb]:border-0 [&::-moz-range-thumb]:bg-charcoal [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-charcoal"
        aria-label="Temperature"
      />
    </div>
  );
}
