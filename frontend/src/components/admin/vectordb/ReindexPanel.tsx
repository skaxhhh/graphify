import { EntityMultiSelect } from "@/components/admin/vectordb/EntityMultiSelect";
import { JobProgressBanner } from "@/components/admin/vectordb/JobProgressBanner";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import type { EmbeddingJob, ReindexScope } from "@/types/vectorDb";

interface ReindexPanelProps {
  scope: ReindexScope;
  onScopeChange: (scope: ReindexScope) => void;
  selectedIds: number[];
  onSelectedIdsChange: (ids: number[]) => void;
  activeJob: EmbeddingJob | null;
  jobError: string | null;
  disabled: boolean;
  onRequestReindex: () => void;
}

export function ReindexPanel({
  scope,
  onScopeChange,
  selectedIds,
  onSelectedIdsChange,
  activeJob,
  jobError,
  disabled,
  onRequestReindex,
}: ReindexPanelProps) {
  const emptySelection = scope === "SELECTED" && selectedIds.length === 0;
  const jobRunning =
    activeJob?.status === "RUNNING" || activeJob?.status === "PENDING";

  return (
    <section className="rounded-xl border border-warm-border bg-cream p-6 shadow-sm">
      <header className="mb-4">
        <h2 className="text-lg font-semibold text-charcoal">재임베딩</h2>
        <p className="mt-1 text-sm text-muted-gray">
          전체 또는 선택한 기업 문서에 대해 벡터 인덱스를 다시 생성합니다.
        </p>
      </header>

      <fieldset className="space-y-3" disabled={disabled || jobRunning}>
        <legend className="sr-only">재임베딩 범위</legend>
        <label className="flex cursor-pointer items-center gap-2 text-sm text-charcoal">
          <input
            type="radio"
            name="reindex-scope"
            className="accent-charcoal"
            checked={scope === "ALL"}
            onChange={() => onScopeChange("ALL")}
          />
          전체 재임베딩
        </label>
        <label className="flex cursor-pointer items-center gap-2 text-sm text-charcoal">
          <input
            type="radio"
            name="reindex-scope"
            className="accent-charcoal"
            checked={scope === "SELECTED"}
            onChange={() => onScopeChange("SELECTED")}
          />
          선택 재임베딩
        </label>
      </fieldset>

      <div className="mt-4">
        <EntityMultiSelect
          scope={scope}
          selectedIds={selectedIds}
          onChange={onSelectedIdsChange}
          disabled={disabled || jobRunning}
        />
      </div>

      {activeJob ? (
        <div className="mt-4">
          <JobProgressBanner
            status={activeJob.status}
            progress={activeJob.progress}
            message={activeJob.message}
            error={jobError}
          />
        </div>
      ) : null}

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <PrimaryButton
          type="button"
          className="!w-auto"
          disabled={disabled || jobRunning || emptySelection}
          onClick={onRequestReindex}
        >
          재임베딩 실행
        </PrimaryButton>
        {emptySelection ? (
          <p className="text-xs text-muted-gray">대상을 선택해야 실행할 수 있습니다.</p>
        ) : null}
      </div>
    </section>
  );
}
