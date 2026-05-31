import { useMemo, type RefObject } from "react";
import {
  extractUsedTokens,
  insertTokenAtCursor,
  PROMPT_VARIABLES_BY_TASK,
  type PromptVariableDef,
} from "@/lib/promptVariables";
import type { PromptTaskType } from "@/types/agentPrompt";

interface PromptVariablePanelProps {
  taskType: PromptTaskType;
  taskTemplate: string;
  onTemplateChange: (value: string) => void;
  textareaRef: RefObject<HTMLTextAreaElement | null>;
  readOnly?: boolean;
}

function VariableChip({
  variable,
  used,
  disabled,
  onInsert,
}: {
  variable: PromptVariableDef;
  used: boolean;
  disabled: boolean;
  onInsert: () => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onInsert}
      className={`rounded-full border px-3 py-1 font-mono text-xs transition-colors ${
        used
          ? "border-charcoal/40 bg-charcoal/10 text-charcoal"
          : "border-warm-border bg-cream-surface text-muted-gray hover:border-charcoal/30 hover:text-charcoal"
      } disabled:cursor-not-allowed disabled:opacity-50`}
      title={variable.description}
    >
      {variable.token}
    </button>
  );
}

export function PromptVariablePanel({
  taskType,
  taskTemplate,
  onTemplateChange,
  textareaRef,
  readOnly = false,
}: PromptVariablePanelProps) {
  const variables = PROMPT_VARIABLES_BY_TASK[taskType];
  const usedTokens = useMemo(() => extractUsedTokens(taskTemplate), [taskTemplate]);

  const handleInsert = (token: string) => {
    const el = textareaRef.current;
    if (!el || readOnly) return;
    insertTokenAtCursor(el, taskTemplate, token, onTemplateChange);
  };

  return (
    <section
      className="rounded-xl border border-warm-border bg-light-cream/40 p-4"
      aria-label="프롬프트 입력 인자"
      data-testid="prompt-variable-panel"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-sm font-semibold text-charcoal">입력 인자</h3>
        <span className="text-xs text-muted-gray">
          칩을 누르면 커서 위치에 삽입 · 템플릿에서 직접 삭제·이동 가능
        </span>
      </div>
      <p className="mt-2 text-xs leading-relaxed text-muted-gray">
        아래 변수는 태스크 실행 시 실제 데이터로 치환됩니다. 원하는 위치에{" "}
        <code className="rounded bg-cream px-1 font-mono text-[11px]">{`{{…}}`}</code> 형태로
        배치하세요.{" "}
        <code className="rounded bg-cream px-1 font-mono text-[11px]">{`{{context}}`}</code>
        ·{" "}
        <code className="rounded bg-cream px-1 font-mono text-[11px]">{`{{market_technical}}`}</code>
        를 템플릿에서 빼면, 조회 가능할 때 서버가 해당 블록을 끝에 자동으로 붙입니다.
      </p>

      <div className="mt-3 flex flex-wrap gap-2" data-testid="prompt-variable-chips">
        {variables.map((v) => (
          <VariableChip
            key={v.token}
            variable={v}
            used={usedTokens.includes(v.token)}
            disabled={readOnly}
            onInsert={() => handleInsert(v.token)}
          />
        ))}
      </div>

      {usedTokens.length > 0 ? (
        <p className="mt-2 text-xs text-charcoal">
          템플릿에 사용 중:{" "}
          {usedTokens.map((t) => (
            <code key={t} className="mr-1 rounded bg-cream px-1 font-mono text-[11px]">
              {t}
            </code>
          ))}
        </p>
      ) : (
        <p className="mt-2 text-xs text-amber-800/90">
          아직 입력 인자가 없습니다. 칩을 눌러 추가하거나 직접 입력하세요.
        </p>
      )}

      <ul className="mt-4 space-y-3 border-t border-warm-border/80 pt-4">
        {variables.map((v) => (
          <li key={v.token} className="text-sm">
            <div className="flex flex-wrap items-baseline gap-2">
              <code className="rounded bg-cream px-1.5 py-0.5 font-mono text-xs text-charcoal">
                {v.token}
              </code>
              <span className="font-medium text-charcoal">{v.label}</span>
              {usedTokens.includes(v.token) ? (
                <span className="rounded bg-charcoal/10 px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-charcoal">
                  사용 중
                </span>
              ) : null}
            </div>
            <p className="mt-1 text-xs leading-relaxed text-muted-gray">{v.description}</p>
            <p className="mt-1 font-mono text-[11px] leading-snug text-muted-gray/90">
              예: {v.examplePreview.slice(0, 120)}
              {v.examplePreview.length > 120 ? "…" : ""}
            </p>
          </li>
        ))}
      </ul>
    </section>
  );
}
