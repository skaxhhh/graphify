import { useNavigate } from "react-router-dom";
import { InsightCard } from "@/components/shared/InsightCard";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { setGraphNavigationFromDetail } from "@/lib/graphSession";
import type { CompanyDetail, InsightCard as InsightCardType } from "@/types/company";

interface InsightCardGridProps {
  company: CompanyDetail;
  cards: InsightCardType[];
  loading: boolean;
  empty: boolean;
  onDetail: (card: InsightCardType) => void;
  onReliability: () => void;
}

export function InsightCardGrid({
  company,
  cards,
  loading,
  empty,
  onDetail,
  onReliability,
}: InsightCardGridProps) {
  const navigate = useNavigate();

  const goToGraphWithHighlight = (card: InsightCardType) => {
    setGraphNavigationFromDetail(company.id, card.highlightNodeIds);
    navigate(`/companies/${company.id}/graph`);
  };

  if (loading) {
    return (
      <section aria-busy="true" aria-label="인사이트 로딩">
        <h2 className="mb-4 text-sm font-semibold text-charcoal">인사이트 요약</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <SkeletonBlock key={index} className="h-40 rounded-xl" />
          ))}
        </div>
      </section>
    );
  }

  if (empty) {
    return (
      <section className="rounded-xl border border-dashed border-warm-border p-8 text-center">
        <p className="text-sm text-muted-gray">
          아직 생성된 인사이트가 없습니다. 관계 그래프에서 분석을 시작해 보세요.
        </p>
        <PrimaryButton
          className="mx-auto mt-4 max-w-xs"
          onClick={() => {
            setGraphNavigationFromDetail(company.id);
            navigate(`/companies/${company.id}/graph`);
          }}
        >
          관계 그래프 보기
        </PrimaryButton>
      </section>
    );
  }

  return (
    <section>
      <h2 className="mb-4 text-sm font-semibold text-charcoal">인사이트 요약</h2>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {cards.map((card) => (
          <InsightCard
            key={card.id}
            card={card}
            onDetail={onDetail}
            onReliability={onReliability}
            onGraphHighlight={goToGraphWithHighlight}
          />
        ))}
      </div>
    </section>
  );
}
