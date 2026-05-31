import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { CheckboxRow } from "@/components/shared/CheckboxRow";
import { TextField } from "@/components/shared/TextField";
import { VECTOR_TYPE_OPTIONS, type VectorEntityType } from "@/types/vectorDb";

interface CleanupPanelProps {
  olderThanDays: number;
  onOlderThanDaysChange: (days: number) => void;
  types: VectorEntityType[];
  onToggleType: (type: VectorEntityType) => void;
  previewCount: number | null;
  previewLoading: boolean;
  disabled: boolean;
  onRequestCleanup: () => void;
}

export function CleanupPanel({
  olderThanDays,
  onOlderThanDaysChange,
  types,
  onToggleType,
  previewCount,
  previewLoading,
  disabled,
  onRequestCleanup,
}: CleanupPanelProps) {
  return (
    <section className="w-full rounded-xl border border-warm-border bg-cream p-6 shadow-sm">
      <header className="mb-4">
        <h2 className="text-lg font-semibold text-charcoal">만료 벡터 정리</h2>
        <p className="mt-1 text-sm text-muted-gray">
          보관 기간을 초과한 벡터를 삭제합니다. 실행 전 미리보기로 대상 건수를 확인하세요.
        </p>
      </header>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="space-y-4">
          <TextField
            id="olderThanDays"
            label="만료 기준 (일)"
            type="number"
            min={1}
            max={3650}
            value={String(olderThanDays)}
            onChange={(e) => onOlderThanDaysChange(Number(e.target.value) || 1)}
            disabled={disabled}
          />
          <fieldset className="space-y-2" disabled={disabled}>
            <legend className="text-sm font-medium text-charcoal">대상 유형</legend>
            {VECTOR_TYPE_OPTIONS.map((opt) => (
              <CheckboxRow
                key={opt.value}
                id={`type-${opt.value}`}
                label={opt.label}
                checked={types.includes(opt.value)}
                onChange={() => onToggleType(opt.value)}
              />
            ))}
          </fieldset>
        </div>

        <div className="rounded-lg border border-warm-border bg-light-cream/30 p-4">
          <h3 className="text-sm font-medium text-charcoal">삭제 미리보기</h3>
          {previewLoading ? (
            <SkeletonBlock className="mt-3 h-12 w-full rounded-md" />
          ) : (
            <p className="mt-3 text-2xl font-semibold text-charcoal">
              {previewCount == null ? "—" : `${previewCount.toLocaleString("ko-KR")}건`}
            </p>
          )}
          <p className="mt-1 text-xs text-muted-gray">
            규칙 변경 시 자동으로 갱신됩니다 (300ms 지연).
          </p>
        </div>
      </div>

      <div className="mt-6">
        <button
          type="button"
          disabled={disabled || types.length === 0 || previewCount === 0}
          onClick={onRequestCleanup}
          className="inline-flex min-h-[44px] items-center justify-center rounded-lg border border-charcoal/40 bg-transparent px-6 text-sm font-medium text-charcoal transition hover:bg-light-cream disabled:cursor-not-allowed disabled:opacity-50"
        >
          만료 벡터 삭제 실행
        </button>
      </div>
    </section>
  );
}
