import { useEffect, useRef, useState } from "react";
import { useSmoothProgress } from "@/hooks/useSmoothProgress";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { AgentInsightPanel } from "@/components/company/AgentInsightPanel";
import {
  COMPANY_DETAIL_COLUMN,
  COMPANY_DETAIL_MAIN_GRID,
  COMPANY_DETAIL_PAGE_SHELL,
} from "@/components/company/companyDetailLayout";
import {
  CompanyDataCollectingSection,
  CompanyDisclosureSection,
  CompanyFinancialSection,
  CompanyNewsSection,
} from "@/components/company/CompanyDataSections";
import { CompanyDetailSkeleton } from "@/components/company/CompanyDetailSkeleton";
import { CompanyHeroCard } from "@/components/company/CompanyHeroCard";
import { CompanyInfoSection } from "@/components/company/CompanyInfoSection";
import { CompanySignalSections } from "@/components/company/CompanySignalSections";
import { CompanyTechnicalPanel } from "@/components/company/CompanyTechnicalPanel";
import { DataProvenanceCard } from "@/components/shared/DataProvenanceCard";
import { DisclaimerCompact } from "@/components/shared/DisclaimerCompact";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { ReliabilityCriteriaModal } from "@/components/shared/ReliabilityCriteriaModal";
import { ApiRequestError } from "@/lib/apiClient";
import {
  fetchCompanyDetail,
  fetchCompanyInsights,
  fetchCompanyMarketTechnical,
  generateCompanyAgentInsight,
  syncCompanyFromDart,
} from "@/lib/companyApi";

function parseCompanyId(raw: string | undefined): number | null {
  if (!raw) return null;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) return null;
  return id;
}

export function CompanyDetailPage() {
  const { companyId: companyIdParam } = useParams<{ companyId: string }>();
  const companyId = parseCompanyId(companyIdParam);
  const queryClient = useQueryClient();

  const [reliabilityOpen, setReliabilityOpen] = useState(false);
  const [pipelineError, setPipelineError] = useState<string | null>(null);
  const pipelineStarted = useRef(false);
  const pipelineProgress = useSmoothProgress();

  const detailQuery = useQuery({
    queryKey: ["company", companyId],
    queryFn: async () => {
      const response = await fetchCompanyDetail(companyId!);
      return response.data;
    },
    enabled: companyId != null,
    retry: 1,
  });

  const insightsQuery = useQuery({
    queryKey: ["company-insights", companyId],
    queryFn: async () => {
      const response = await fetchCompanyInsights(companyId!);
      return response.data;
    },
    enabled: companyId != null && detailQuery.isSuccess,
    retry: 1,
  });

  const marketTechnicalQuery = useQuery({
    queryKey: ["company-market-technical", companyId],
    queryFn: async () => {
      const response = await fetchCompanyMarketTechnical(companyId!);
      return response.data;
    },
    enabled:
      companyId != null && detailQuery.isSuccess && Boolean(detailQuery.data?.ticker),
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code === "ERR_COMPANY_006") {
        return false;
      }
      return failureCount < 1;
    },
  });

  const dartPipeline = useMutation({
    mutationFn: async (id: number) => {
      const company = detailQuery.data;
      const profile = company?.dartProfile;
      const missingRelatedData =
        profile != null &&
        profile.recentDisclosures.length === 0 &&
        profile.relatedNews.length === 0;
      const shouldSync =
        company?.needsSync === true ||
        !profile ||
        missingRelatedData ||
        profile.financialStatements.length === 0;

      pipelineProgress.begin("준비 중…", 8);

      if (shouldSync) {
        pipelineProgress.setCap(48, "DART 데이터 수집 중…");
        await syncCompanyFromDart(id);
      }

      pipelineProgress.setCap(92, "AI 인사이트·신호 생성 중…");
      await generateCompanyAgentInsight(id);
    },
    onMutate: () => {
      pipelineProgress.reset();
      pipelineProgress.begin("시작…", 5);
    },
    onSuccess: async () => {
      setPipelineError(null);
      pipelineProgress.finish();
      await queryClient.invalidateQueries({ queryKey: ["company", companyId] });
      await queryClient.invalidateQueries({ queryKey: ["company-insights", companyId] });
    },
    onError: (error: Error) => {
      pipelineProgress.reset();
      setPipelineError(error.message || "DART 수집 또는 인사이트 생성에 실패했습니다.");
    },
  });

  const company = detailQuery.data;
  const insights = insightsQuery.data;
  const profile = company?.dartProfile;
  const profileIncomplete =
    profile != null &&
    (profile.financialStatements.length === 0 ||
      (profile.recentDisclosures.length === 0 && profile.relatedNews.length === 0));

  const needsAgentRegenerate =
    !insights?.agentInsight ||
    insights.agentInsight.modelLabel === "mock-dev" ||
    (profile != null && (insights.signals.length === 0));

  const needsDartPipeline =
    company != null &&
    (company.needsSync === true ||
      !profile ||
      profileIncomplete ||
      needsAgentRegenerate);

  useEffect(() => {
    if (companyId == null || !company || !needsDartPipeline) {
      return;
    }
    if (pipelineStarted.current || dartPipeline.isPending) {
      return;
    }
    pipelineStarted.current = true;
    dartPipeline.mutate(companyId);
  }, [companyId, company, needsDartPipeline, dartPipeline]);

  const pageState: PageStateKind = (() => {
    if (companyId == null) return "empty";
    if (detailQuery.isLoading) return "loading";
    if (detailQuery.isError) return "error";
    if (!detailQuery.data) return "empty";
    return "populated";
  })();

  const insightLoading =
    insightsQuery.isLoading ||
    dartPipeline.isPending ||
    (needsDartPipeline && !insights?.agentInsight);

  const shellClass = COMPANY_DETAIL_PAGE_SHELL;

  return (
    <>
      <PageState
        state={pageState}
        loading={<CompanyDetailSkeleton />}
        empty={
          <div className={shellClass}>
            <EmptyState
              title="기업을 찾을 수 없습니다"
              description="검색 결과에서 다시 선택하거나 URL을 확인해 주세요."
            />
            <p className="mt-4 text-center">
              <Link to="/search" className="text-sm text-charcoal underline hover:opacity-80">
                검색으로 돌아가기
              </Link>
            </p>
          </div>
        }
        error={
          <div className={shellClass}>
            <ErrorBanner
              message={
                detailQuery.error instanceof Error
                  ? detailQuery.error.message
                  : "기업 정보를 불러오지 못했습니다."
              }
              onRetry={() => detailQuery.refetch()}
            />
            <Link to="/search" className="text-sm text-muted-gray underline">
              검색으로 돌아가기
            </Link>
          </div>
        }
      >
        {company ? (
          <div className={`${shellClass} space-y-8`}>
            <CompanyHeroCard company={company} />

            <div className={COMPANY_DETAIL_MAIN_GRID} data-testid="company-detail-layout">
              {/* 좌측: 기본정보 → 재무 → 공시 → 데이터 출처 */}
              <div className={COMPANY_DETAIL_COLUMN} data-testid="company-detail-left">
                <CompanyInfoSection company={company} />
                {profile ? (
                  <>
                    <CompanyFinancialSection profile={profile} />
                    <CompanyDisclosureSection profile={profile} />
                  </>
                ) : insightLoading ? (
                  <CompanyDataCollectingSection />
                ) : null}
                <DataProvenanceCard
                  provenance={company.provenance}
                  onReliabilityClick={() => setReliabilityOpen(true)}
                />
                <DisclaimerCompact />
              </div>

              {/* 우측: 뉴스 → 리스크 → 기회 → AI 인사이트 */}
              <div className={COMPANY_DETAIL_COLUMN} data-testid="company-detail-right">
                {profile ? <CompanyNewsSection profile={profile} /> : null}
                <CompanyTechnicalPanel
                  data={marketTechnicalQuery.data ?? undefined}
                  loading={marketTechnicalQuery.isLoading}
                  errorMessage={
                    marketTechnicalQuery.error instanceof ApiRequestError
                      ? marketTechnicalQuery.error.message
                      : marketTechnicalQuery.isError
                        ? "시장 지표를 불러오지 못했습니다."
                        : null
                  }
                />
                <CompanySignalSections signals={insights?.signals ?? []} />
                <AgentInsightPanel
                  insight={insights?.agentInsight}
                  loading={insightLoading}
                  loadingPercent={
                    dartPipeline.isPending || insightLoading
                      ? pipelineProgress.percent || 3
                      : undefined
                  }
                  loadingLabel={pipelineProgress.label || undefined}
                  errorMessage={pipelineError}
                  onRetry={() => {
                    if (companyId == null) return;
                    pipelineStarted.current = false;
                    dartPipeline.mutate(companyId);
                  }}
                />
              </div>
            </div>
          </div>
        ) : null}
      </PageState>

      <ReliabilityCriteriaModal open={reliabilityOpen} onClose={() => setReliabilityOpen(false)} />
    </>
  );
}
