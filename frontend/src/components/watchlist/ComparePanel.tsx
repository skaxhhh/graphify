import { BasisTabs } from "@/components/watchlist/BasisTabs";
import { InsightCompareColumn } from "@/components/watchlist/InsightCompareColumn";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import type { CompanyCompareData, CompareBasis } from "@/types/watchlist";

interface ComparePanelProps {
  open: boolean;
  basis: CompareBasis;
  onBasisChange: (basis: CompareBasis) => void;
  data: CompanyCompareData | undefined;
  loading: boolean;
  error: boolean;
  onRetry: () => void;
}

export function ComparePanel({
  open,
  basis,
  onBasisChange,
  data,
  loading,
  error,
  onRetry,
}: ComparePanelProps) {
  if (!open) return null;

  return (
    <section
      className="mt-8 animate-[slideUp_250ms_ease-out] rounded-xl border border-warm-border bg-cream p-4 md:p-6"
      aria-label="기업 비교"
    >
      <BasisTabs value={basis} onChange={onBasisChange} />

      {loading ? (
        <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <SkeletonBlock key={i} className="min-h-[280px] rounded-xl" />
          ))}
        </div>
      ) : null}

      {error ? (
        <p className="mt-6 text-sm text-muted-gray">
          비교 데이터를 불러오지 못했습니다.{" "}
          <button type="button" className="underline" onClick={onRetry}>
            재시도
          </button>
        </p>
      ) : null}

      {!loading && !error && data ? (
        <div className="mt-6 flex snap-x snap-mandatory gap-4 overflow-x-auto pb-2 md:grid md:grid-cols-3 md:overflow-visible">
          {data.companies.map((company) => (
            <InsightCompareColumn key={company.companyId} company={company} />
          ))}
        </div>
      ) : null}
    </section>
  );
}
