import { useNavigate } from "react-router-dom";
import { GhostButton } from "@/components/shared/GhostButton";
import { DataFreshnessBadges } from "@/components/shared/DataFreshnessBadges";
import { IndustryBadge } from "@/components/shared/IndustryBadge";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { WatchToggle } from "@/components/shared/WatchToggle";
import { setGraphNavigationFromDetail } from "@/lib/graphSession";
import type { CompanyDetail } from "@/types/company";

interface CompanyHeroCardProps {
  company: CompanyDetail;
}

export function CompanyHeroCard({ company }: CompanyHeroCardProps) {
  const navigate = useNavigate();

  const goToGraph = () => {
    setGraphNavigationFromDetail(company.id);
    navigate(`/companies/${company.id}/graph`);
  };

  return (
    <section className="w-full rounded-xl border border-warm-border bg-cream p-6 md:p-8">
      <div className="flex flex-col gap-8 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0 flex-1 space-y-3">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight text-charcoal md:text-4xl">
              {company.name}
            </h1>
            {company.ticker ? (
              <p className="mt-1 text-sm text-muted-gray">{company.ticker}</p>
            ) : null}
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <IndustryBadge industry={company.industry} />
            <DataFreshnessBadges
              dataStatus={company.dataStatus}
              coverage={company.coverageByRelationType}
            />
          </div>
        </div>
        <div className="flex w-full flex-col gap-3 sm:flex-row lg:w-auto lg:flex-col lg:items-stretch">
          <PrimaryButton className="lg:min-w-[200px]" onClick={goToGraph}>
            관계 그래프 보기
          </PrimaryButton>
          <GhostButton
            className="lg:min-w-[200px]"
            onClick={() => navigate("/app/history")}
          >
            분석 이력
          </GhostButton>
          <WatchToggle companyId={company.id} />
        </div>
      </div>
    </section>
  );
}
