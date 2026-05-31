interface SystemPromptEditorProps {
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
}

export function SystemPromptEditor({
  value,
  onChange,
  readOnly = false,
}: SystemPromptEditorProps) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-charcoal">시스템 프롬프트</label>
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        readOnly={readOnly}
        className="min-h-[240px] w-full resize-y rounded-md border border-warm-border bg-cream-surface px-3 py-3 font-mono text-sm text-charcoal placeholder:text-muted-gray focus:outline-none focus:ring-2 focus:ring-ring-blue read-only:opacity-70"
        placeholder="Agent의 기본 역할·출력 형식을 정의합니다."
      />
    </div>
  );
}
