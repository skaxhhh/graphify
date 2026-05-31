import { forwardRef } from "react";

interface TaskTemplateEditorProps {
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
}

export const TaskTemplateEditor = forwardRef<HTMLTextAreaElement, TaskTemplateEditorProps>(
  function TaskTemplateEditor({ value, onChange, readOnly = false }, ref) {
    return (
      <div className="space-y-2">
        <label className="text-sm font-medium text-charcoal" htmlFor="task-template">
          태스크 템플릿
        </label>
        <textarea
          id="task-template"
          ref={ref}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          readOnly={readOnly}
          className="min-h-[200px] w-full resize-y rounded-md border border-warm-border bg-cream-surface px-3 py-3 font-mono text-sm text-charcoal placeholder:text-muted-gray focus:outline-none focus:ring-2 focus:ring-ring-blue read-only:opacity-70"
          placeholder="입력 인자(예: {{company_name}}, {{context}})를 원하는 위치에 배치하세요."
        />
      </div>
    );
  }
);
