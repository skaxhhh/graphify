import { PROMPT_TASK_TABS, type PromptTaskType } from "@/types/agentPrompt";

interface TaskTypeTabsProps {
  active: PromptTaskType;
  onChange: (type: PromptTaskType) => void;
  disabled?: boolean;
}

export function TaskTypeTabs({ active, onChange, disabled }: TaskTypeTabsProps) {
  return (
    <div
      className="flex h-12 shrink-0 gap-1 border-b border-warm-border px-1"
      role="tablist"
      aria-label="프롬프트 태스크 유형"
    >
      {PROMPT_TASK_TABS.map((tab) => {
        const isActive = tab.type === active;
        return (
          <button
            key={tab.type}
            type="button"
            role="tab"
            aria-selected={isActive}
            disabled={disabled}
            onClick={() => onChange(tab.type)}
            className={`relative px-4 py-3 text-sm transition-colors disabled:opacity-50 ${
              isActive
                ? "font-medium text-charcoal after:absolute after:inset-x-2 after:bottom-0 after:h-0.5 after:bg-charcoal"
                : "text-muted-gray hover:text-charcoal"
            }`}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
