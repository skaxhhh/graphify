import { PromptEditor } from "@/components/mypage/PromptEditor";

interface PremiumPromptSectionProps {
  customPrompt: string;
  onChange: (value: string) => void;
  onSave: () => void;
  saving: boolean;
}

export function PremiumPromptSection({
  customPrompt,
  onChange,
  onSave,
  saving,
}: PremiumPromptSectionProps) {
  return (
    <section className="rounded-xl border border-warm-border bg-cream p-6">
      <h2 className="text-base font-semibold text-charcoal">커스텀 프롬프트</h2>
      <p className="mt-1 text-xs text-muted-gray">
        Premium 전용 · Agent 분석 시 우선 적용되는 지침입니다.
      </p>
      <div className="mt-6">
        <PromptEditor
          value={customPrompt}
          onChange={onChange}
          onSave={onSave}
          saving={saving}
        />
      </div>
    </section>
  );
}
