import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

interface PromptEditorProps {
  value: string;
  onChange: (value: string) => void;
  onSave: () => void;
  saving: boolean;
  disabled?: boolean;
}

export function PromptEditor({
  value,
  onChange,
  onSave,
  saving,
  disabled = false,
}: PromptEditorProps) {
  return (
    <div className="space-y-4">
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled || saving}
        placeholder="Agent 분석 시 참고할 지침을 입력하세요."
        className="min-h-[160px] w-full resize-y rounded-md border border-warm-border bg-cream-surface px-3 py-3 text-sm text-charcoal placeholder:text-muted-gray focus:outline-none focus:ring-2 focus:ring-ring-blue disabled:opacity-60"
      />
      {saving ? (
        <SkeletonBlock className="h-1 w-full rounded-full" aria-hidden />
      ) : null}
      <PrimaryButton
        type="button"
        loading={saving}
        disabled={disabled}
        className="md:!w-auto md:px-8"
        onClick={onSave}
      >
        프롬프트 저장
      </PrimaryButton>
    </div>
  );
}
