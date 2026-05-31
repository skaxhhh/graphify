import { PrimaryButton } from "@/components/shared/PrimaryButton";

interface CompareDockProps {
  selectedNames: string[];
  onCompare: () => void;
  onClear: () => void;
}

export function CompareDock({ selectedNames, onCompare, onClear }: CompareDockProps) {
  if (selectedNames.length === 0) return null;

  return (
    <div className="sticky bottom-0 z-40 -mx-4 border-t border-warm-border bg-cream px-4 py-4 md:-mx-8 md:px-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap gap-2">
          {selectedNames.map((name) => (
            <span
              key={name}
              className="rounded-full border border-warm-border bg-charcoal/[0.03] px-3 py-1 text-xs text-charcoal"
            >
              {name}
            </span>
          ))}
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onClear}
            className="rounded-md border border-warm-border px-4 py-2 text-sm text-muted-gray hover:bg-charcoal/[0.03]"
          >
            선택 해제
          </button>
          <PrimaryButton className="!w-auto px-6" onClick={onCompare}>
            비교 보기
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
}
