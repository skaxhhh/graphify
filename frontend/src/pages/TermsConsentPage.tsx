import { useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Navigate, useNavigate } from "react-router-dom";
import { CheckboxRow } from "@/components/shared/CheckboxRow";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { GhostButton } from "@/components/shared/GhostButton";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { TermsDetailModal } from "@/components/terms/TermsDetailModal";
import { useAuthHydration } from "@/hooks/useAuthHydration";
import { needsTermsConsent } from "@/lib/authRedirect";
import { ApiRequestError } from "@/lib/apiClient";
import { submitConsent } from "@/lib/authApi";
import { fetchTermsLatest } from "@/lib/searchApi";
import { useAuthStore } from "@/stores/authStore";
import type { TermItem } from "@/types/search";

export function TermsConsentPage() {
  const navigate = useNavigate();
  const hydrated = useAuthHydration();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const updateUser = useAuthStore((s) => s.updateUser);

  const [checkedIds, setCheckedIds] = useState<Set<number>>(new Set());
  const [consentError, setConsentError] = useState<string | null>(null);
  const [detailTerm, setDetailTerm] = useState<TermItem | null>(null);
  const [successToast, setSuccessToast] = useState(false);

  const termsQuery = useQuery({
    queryKey: ["terms", "latest", "consent"],
    queryFn: async () => {
      const response = await fetchTermsLatest();
      return response.data;
    },
    enabled: hydrated && isAuthenticated,
  });

  const terms = termsQuery.data?.terms ?? [];
  const requiredTerms = useMemo(
    () => terms.filter((term) => term.required),
    [terms]
  );

  const allRequiredChecked =
    requiredTerms.length > 0 &&
    requiredTerms.every((term) => checkedIds.has(term.id));

  const masterChecked =
    requiredTerms.length > 0 &&
    requiredTerms.every((term) => checkedIds.has(term.id));

  const consentMutation = useMutation({
    mutationFn: async () => {
      if (!termsQuery.data) {
        throw new ApiRequestError("ERR_TERMS_000", "약관 정보가 없습니다.");
      }
      const response = await submitConsent({
        acceptedTermIds: Array.from(checkedIds),
        version: termsQuery.data.version,
      });
      if (!response.data) {
        throw new ApiRequestError("ERR_TERMS_000", "동의 처리에 실패했습니다.");
      }
      return response.data;
    },
    onSuccess: (data) => {
      updateUser(data.user);
      setConsentError(null);
      setSuccessToast(true);
      window.setTimeout(() => {
        navigate("/", { replace: true });
      }, 600);
    },
    onError: (error) => {
      if (error instanceof ApiRequestError) {
        setConsentError(error.message);
      } else {
        setConsentError("약관 동의 처리에 실패했습니다. 다시 시도해 주세요.");
      }
    },
  });

  if (hydrated && !isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (hydrated && user && !needsTermsConsent(user)) {
    return <Navigate to="/" replace />;
  }

  const pageState: PageStateKind = (() => {
    if (!hydrated) return "loading";
    if (termsQuery.isLoading) return "loading";
    if (termsQuery.isError) return "error";
    if (terms.length === 0) return "empty";
    return "populated";
  })();

  const toggleMaster = (checked: boolean) => {
    if (checked) {
      setCheckedIds(new Set(requiredTerms.map((term) => term.id)));
    } else {
      setCheckedIds(new Set());
    }
    setConsentError(null);
  };

  const toggleTerm = (termId: number, checked: boolean) => {
    setCheckedIds((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(termId);
      } else {
        next.delete(termId);
      }
      return next;
    });
    setConsentError(null);
  };

  const handleSubmit = () => {
    if (!allRequiredChecked) {
      setConsentError("필수 약관에 모두 동의해 주세요.");
      return;
    }
    consentMutation.mutate();
  };

  const handleBack = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <>
      <div className="flex flex-1 flex-col items-center px-4 py-16 md:py-24">
        <PageState
          state={pageState}
          loading={
            <div className="w-full max-w-[560px] space-y-4 rounded-xl border border-warm-border bg-cream p-6 md:p-8">
              <SkeletonBlock className="h-6 w-2/3" />
              <SkeletonBlock className="h-4 w-full" />
              <SkeletonBlock className="h-4 w-full" />
              <SkeletonBlock className="h-10 w-full" />
            </div>
          }
          error={
            <div className="w-full max-w-[560px] rounded-xl border border-warm-border bg-cream p-6 text-center md:p-8">
              <p className="text-charcoal">약관을 불러오지 못했습니다.</p>
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
            <div className="w-full max-w-[560px] rounded-xl border border-warm-border bg-cream p-6 text-center text-muted-gray md:p-8">
              표시할 약관이 없습니다.
            </div>
          }
        >
          <div className="w-full max-w-[560px] rounded-xl border border-warm-border bg-cream p-6 md:p-8">
            <p className="text-sm leading-relaxed text-muted-gray">
              서비스 이용을 위해 약관에 동의해 주세요.
            </p>

            <div className="mt-6 border-b border-warm-border pb-4">
              <CheckboxRow
                id="master-consent"
                label="필수 약관 전체 동의"
                checked={masterChecked}
                disabled={consentMutation.isPending}
                onChange={toggleMaster}
              />
            </div>

            <div className="mt-4 max-h-[40vh] overflow-y-auto rounded-md border border-warm-border p-4 md:max-h-[320px]">
              <ul className="space-y-4">
                {terms.map((term) => (
                  <li
                    key={term.id}
                    className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <CheckboxRow
                      id={`term-${term.id}`}
                      label={`${term.title}${term.required ? " (필수)" : ""}`}
                      checked={checkedIds.has(term.id)}
                      disabled={consentMutation.isPending}
                      onChange={(checked) => toggleTerm(term.id, checked)}
                    />
                    <button
                      type="button"
                      onClick={() => setDetailTerm(term)}
                      className="text-left text-sm text-muted-gray underline underline-offset-2 hover:text-charcoal sm:shrink-0"
                    >
                      본문 보기
                    </button>
                  </li>
                ))}
              </ul>
            </div>

            {consentError ? (
              <div className="mt-4 animate-[slideDown_0.2s_ease-out]">
                <ErrorBanner
                  message={consentError}
                  onRetry={() => setConsentError(null)}
                />
              </div>
            ) : null}

            <div className="mt-8">
              <PrimaryButton
                type="button"
                className="w-full"
                loading={consentMutation.isPending}
                disabled={!allRequiredChecked || consentMutation.isPending}
                onClick={handleSubmit}
              >
                동의하고 시작하기
              </PrimaryButton>
            </div>

            <GhostButton fullWidth className="mt-3" onClick={handleBack}>
              뒤로
            </GhostButton>
          </div>
        </PageState>

        {successToast ? (
          <p
            role="status"
            className="fixed bottom-8 left-1/2 z-50 -translate-x-1/2 rounded-md bg-charcoal px-4 py-2 text-sm text-off-white"
          >
            약관 동의가 완료되었습니다.
          </p>
        ) : null}
      </div>

      <TermsDetailModal term={detailTerm} onClose={() => setDetailTerm(null)} />
    </>
  );
}
