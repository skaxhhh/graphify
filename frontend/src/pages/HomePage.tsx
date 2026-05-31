import { useQuery } from "@tanstack/react-query";
import { Navigate } from "react-router-dom";
import { HomeInsightsSection } from "@/components/home/HomeInsightsSection";
import { HeroSection } from "@/components/home/HeroSection";
import { GlobalSearchBar } from "@/components/shared/GlobalSearchBar";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { RecentSearchChips } from "@/components/shared/RecentSearchChips";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { useRecentSearches } from "@/hooks/useRecentSearches";
import { needsTermsConsent } from "@/lib/authRedirect";
import { fetchTermsLatest } from "@/lib/searchApi";
import { useAuthStore } from "@/stores/authStore";
import type { AutocompleteItem } from "@/types/search";

export function HomePage() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const hydrated = useAuthStore((s) => s.hydrated);
  const user = useAuthStore((s) => s.user);
  const { addEntry } = useRecentSearches();

  if (hydrated && isAuthenticated && user && needsTermsConsent(user)) {
    return <Navigate to="/terms" replace />;
  }

  const termsQuery = useQuery({
    queryKey: ["terms", "latest"],
    queryFn: async () => {
      const response = await fetchTermsLatest();
      return response.data;
    },
  });

  const pageState: PageStateKind = (() => {
    if (termsQuery.isLoading) return "loading";
    if (termsQuery.isError) return "error";
    if (termsQuery.data && termsQuery.data.companyCount === 0) return "empty";
    return "populated";
  })();

  const handleCompanySelect = (item: AutocompleteItem) => {
    if (isAuthenticated) {
      addEntry(item.id, item.name);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem-12rem)] flex-col items-center">
      <PageState
        state={pageState}
        loading={
          <div className="w-full max-w-[1200px] px-4 py-24 md:py-32">
            <SkeletonBlock className="mx-auto h-12 w-3/4 max-w-xl" />
            <SkeletonBlock className="mx-auto mt-6 h-6 w-1/2 max-w-md" />
            <SkeletonBlock className="mx-auto mt-12 h-14 w-full max-w-[720px]" />
          </div>
        }
        error={
          <div className="flex flex-col items-center px-4 py-24 text-center">
            <p className="text-charcoal">서비스 정보를 불러오지 못했습니다.</p>
            <button
              type="button"
              onClick={() => void termsQuery.refetch()}
              className="mt-4 rounded-md border border-charcoal/40 px-4 py-2 text-sm text-charcoal hover:opacity-80"
            >
              다시 시도
            </button>
          </div>
        }
        empty={
          <div className="flex flex-col items-center px-4 py-24 text-center">
            <p className="text-muted-gray">
              등록된 기업 데이터가 없습니다. 잠시 후 다시 확인해 주세요.
            </p>
          </div>
        }
      >
        <HeroSection />

        <div className="mx-auto mt-2 w-full max-w-[720px] px-4">
          <GlobalSearchBar
            variant="hero"
            onCompanySelect={handleCompanySelect}
          />
          {isAuthenticated ? <RecentSearchChips /> : null}
        </div>

        <HomeInsightsSection />
      </PageState>
    </div>
  );
}
