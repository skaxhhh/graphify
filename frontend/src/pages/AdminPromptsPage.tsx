import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { PromptVariablePanel } from "@/components/admin/prompts/PromptVariablePanel";
import { PromptEditorSkeleton } from "@/components/admin/prompts/PromptEditorSkeleton";
import { PromptTestPanel } from "@/components/admin/prompts/PromptTestPanel";
import { RollbackConfirmDialog } from "@/components/admin/prompts/RollbackConfirmDialog";
import { SystemPromptEditor } from "@/components/admin/prompts/SystemPromptEditor";
import { TaskTemplateEditor } from "@/components/admin/prompts/TaskTemplateEditor";
import { TaskTypeTabs } from "@/components/admin/prompts/TaskTypeTabs";
import { VersionHistoryPanel } from "@/components/admin/prompts/VersionHistoryPanel";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { GhostButton } from "@/components/shared/GhostButton";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { ApiRequestError } from "@/lib/apiClient";
import {
  fetchAdminPrompt,
  rollbackAdminPrompt,
  saveAdminPrompt,
  testAdminPrompt,
} from "@/lib/adminPromptApi";
import {
  DEFAULT_PROMPT_DRAFTS,
  PROMPT_TASK_TABS,
  type AgentPromptTestResult,
  type AgentPromptVersion,
  type PromptTaskType,
} from "@/types/agentPrompt";

function parseTaskType(raw: string | null): PromptTaskType {
  const found = PROMPT_TASK_TABS.find((t) => t.type === raw);
  return found?.type ?? "RELATION_ANALYSIS";
}

export function AdminPromptsPage() {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const taskType = parseTaskType(searchParams.get("type"));

  const [systemPrompt, setSystemPrompt] = useState("");
  const [taskTemplate, setTaskTemplate] = useState("");
  const [baseline, setBaseline] = useState({ systemPrompt: "", taskTemplate: "" });
  const [changeNote, setChangeNote] = useState("");
  const [promptId, setPromptId] = useState<number | null>(null);
  const [versions, setVersions] = useState<AgentPromptVersion[]>([]);

  const [testOpen, setTestOpen] = useState(false);
  const [testResult, setTestResult] = useState<AgentPromptTestResult | null>(null);
  const [mobileVersionsOpen, setMobileVersionsOpen] = useState(false);

  const [rollbackTarget, setRollbackTarget] = useState<AgentPromptVersion | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const taskTemplateRef = useRef<HTMLTextAreaElement>(null);

  const promptQuery = useQuery({
    queryKey: ["admin", "prompts", taskType],
    queryFn: () => fetchAdminPrompt(taskType),
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError) {
        if (error.code === "ERR_ADMIN_PROMPT_001") return false;
        if (error.code.startsWith("ERR_AUTH")) return false;
      }
      return failureCount < 1;
    },
  });

  const isNotFound =
    promptQuery.error instanceof ApiRequestError &&
    promptQuery.error.code === "ERR_ADMIN_PROMPT_001";

  const applyDraft = useCallback(
    (system: string, task: string, id: number | null, vers: AgentPromptVersion[]) => {
      setSystemPrompt(system);
      setTaskTemplate(task);
      setBaseline({ systemPrompt: system, taskTemplate: task });
      setPromptId(id);
      setVersions(vers);
      setChangeNote("");
      setTestResult(null);
    },
    []
  );

  useEffect(() => {
    if (promptQuery.data?.data) {
      const d = promptQuery.data.data;
      applyDraft(d.systemPrompt, d.taskTemplate, d.id, d.versions);
      return;
    }
    if (isNotFound) {
      const defaults = DEFAULT_PROMPT_DRAFTS[taskType];
      applyDraft(defaults.systemPrompt, defaults.taskTemplate, null, []);
    }
  }, [applyDraft, isNotFound, promptQuery.data, taskType]);

  const dirty =
    systemPrompt !== baseline.systemPrompt || taskTemplate !== baseline.taskTemplate;

  const pageState: PageStateKind = useMemo(() => {
    if (promptQuery.isLoading) return "loading";
    if (promptQuery.isError && !isNotFound) return "error";
    if (isNotFound) return "empty";
    return "populated";
  }, [isNotFound, promptQuery.isError, promptQuery.isLoading]);

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["admin", "prompts", taskType] });

  const saveMutation = useMutation({
    mutationFn: () =>
      saveAdminPrompt({
        type: taskType,
        systemPrompt,
        taskTemplate,
        changeNote: changeNote.trim() || undefined,
      }),
    onSuccess: async (res) => {
      const d = res.data;
      if (d) {
        applyDraft(d.systemPrompt, d.taskTemplate, d.id, d.versions);
      }
      setToast("저장되었습니다.");
      await invalidate();
    },
  });

  const rollbackMutation = useMutation({
    mutationFn: (targetVersionId: number) => {
      if (promptId == null) throw new Error("missing prompt id");
      return rollbackAdminPrompt(promptId, { targetVersionId });
    },
    onSuccess: async (res) => {
      const d = res.data;
      if (d) {
        applyDraft(d.systemPrompt, d.taskTemplate, d.id, d.versions);
      }
      setRollbackTarget(null);
      setToast("롤백되었습니다.");
      await invalidate();
    },
  });

  const testMutation = useMutation({
    mutationFn: ({
      companyId,
      sampleInput,
    }: {
      companyId: number;
      sampleInput: string;
    }) => {
      if (promptId == null) throw new Error("missing prompt id");
      return testAdminPrompt(promptId, { companyId, sampleInput: sampleInput || undefined });
    },
    onSuccess: (res) => {
      setTestResult(res.data ?? null);
    },
  });

  const handleTabChange = (next: PromptTaskType) => {
    if (next === taskType) return;
    if (dirty) {
      const ok = window.confirm(
        "저장하지 않은 변경 사항이 있습니다. 탭을 전환하시겠습니까?"
      );
      if (!ok) return;
    }
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set("type", next);
    setSearchParams(nextParams, { replace: true });
    setTestOpen(false);
    setMobileVersionsOpen(false);
  };

  const editorReadOnly = saveMutation.isPending || rollbackMutation.isPending;

  const editorBody = (
    <div className="flex min-h-0 flex-1 flex-col lg:flex-row">
      <div className="flex min-w-0 flex-1 flex-col gap-4 overflow-y-auto p-4 md:p-6">
        <SystemPromptEditor
          value={systemPrompt}
          onChange={setSystemPrompt}
          readOnly={editorReadOnly}
        />
        <PromptVariablePanel
          taskType={taskType}
          taskTemplate={taskTemplate}
          onTemplateChange={setTaskTemplate}
          textareaRef={taskTemplateRef}
          readOnly={editorReadOnly}
        />
        <TaskTemplateEditor
          ref={taskTemplateRef}
          value={taskTemplate}
          onChange={setTaskTemplate}
          readOnly={editorReadOnly}
        />
        <div className="space-y-2">
          <label className="text-sm text-muted-gray" htmlFor="change-note">
            변경 메모 (선택)
          </label>
          <input
            id="change-note"
            type="text"
            value={changeNote}
            onChange={(e) => setChangeNote(e.target.value)}
            disabled={editorReadOnly}
            className="w-full rounded-md border border-warm-border bg-cream-surface px-3 py-2 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue disabled:opacity-60"
            placeholder="버전 요약에 포함됩니다"
          />
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <PrimaryButton
            type="button"
            className="!w-auto md:px-8"
            loading={saveMutation.isPending}
            disabled={editorReadOnly || (promptId != null && !dirty)}
            onClick={() => saveMutation.mutate()}
          >
            저장
          </PrimaryButton>
          {dirty ? (
            <span className="rounded-md bg-charcoal/10 px-2 py-1 text-xs font-medium text-charcoal">
              변경됨
            </span>
          ) : null}
          <GhostButton
            type="button"
            className="!w-auto"
            disabled={promptId == null}
            onClick={() => setTestOpen(true)}
          >
            테스트 실행
          </GhostButton>
          <button
            type="button"
            className="text-sm text-charcoal underline lg:hidden"
            onClick={() => setMobileVersionsOpen(true)}
          >
            버전 이력
          </button>
        </div>
        {saveMutation.isError ? (
          <p className="text-sm text-red-600">
            {saveMutation.error instanceof ApiRequestError
              ? saveMutation.error.message
              : "저장에 실패했습니다."}
          </p>
        ) : null}
      </div>
      <div className="hidden min-h-0 lg:flex">
        <VersionHistoryPanel
          versions={versions}
          rollingBackId={
            rollbackMutation.isPending ? rollbackTarget?.id ?? null : null
          }
          onRollback={setRollbackTarget}
        />
      </div>
    </div>
  );

  return (
    <div className="-m-4 flex min-h-[calc(100vh-3.5rem)] flex-col md:-m-8">
      <div className="border-b border-warm-border bg-cream px-4 md:px-6">
        <div className="py-4">
          <h1 className="text-xl font-semibold text-charcoal">Agent 프롬프트</h1>
          <p className="mt-1 text-sm text-muted-gray">
            태스크별 시스템·템플릿 프롬프트와 버전을 관리합니다.
          </p>
        </div>
        <TaskTypeTabs
          active={taskType}
          onChange={handleTabChange}
          disabled={promptQuery.isLoading || saveMutation.isPending}
        />
      </div>

      {toast ? (
        <p className="mx-4 mt-3 rounded-lg border border-warm-border bg-light-cream/60 px-4 py-2 text-sm text-charcoal md:mx-6">
          {toast}
        </p>
      ) : null}

      <PageState
        state={pageState}
        loading={<div className="p-4 md:p-6"><PromptEditorSkeleton /></div>}
        empty={
          <div className="flex min-h-0 flex-1 flex-col">
            <div className="p-4 md:p-6">
              <EmptyState
                title="이 태스크의 프롬프트가 아직 없습니다"
                description="아래 초안을 검토한 뒤 저장하면 첫 버전이 생성됩니다."
              />
            </div>
            {editorBody}
          </div>
        }
        error={
          <div className="p-4 md:p-6">
            <ErrorBanner
              message={
                promptQuery.error instanceof ApiRequestError
                  ? promptQuery.error.message
                  : "프롬프트를 불러오지 못했습니다."
              }
              onRetry={() => void promptQuery.refetch()}
            />
          </div>
        }
      >
        {editorBody}
      </PageState>

      {mobileVersionsOpen ? (
        <div className="fixed inset-0 z-40 flex flex-col justify-end lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-charcoal/30"
            aria-label="버전 패널 닫기"
            onClick={() => setMobileVersionsOpen(false)}
          />
          <div className="relative max-h-[70vh] rounded-t-xl border border-warm-border bg-cream shadow-lg">
            <VersionHistoryPanel
              versions={versions}
              rollingBackId={
                rollbackMutation.isPending ? rollbackTarget?.id ?? null : null
              }
              onRollback={(v) => {
                setRollbackTarget(v);
                setMobileVersionsOpen(false);
              }}
            />
          </div>
        </div>
      ) : null}

      <PromptTestPanel
        open={testOpen}
        onClose={() => setTestOpen(false)}
        testing={testMutation.isPending}
        testResult={testResult}
        onRunTest={(companyId, sampleInput) => testMutation.mutate({ companyId, sampleInput })}
      />

      <RollbackConfirmDialog
        open={rollbackTarget != null}
        versionLabel={
          rollbackTarget ? `v${rollbackTarget.versionNumber}` : ""
        }
        loading={rollbackMutation.isPending}
        onClose={() => setRollbackTarget(null)}
        onConfirm={() => {
          if (rollbackTarget) rollbackMutation.mutate(rollbackTarget.id);
        }}
      />
    </div>
  );
}
