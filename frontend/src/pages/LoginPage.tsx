import { useQuery } from "@tanstack/react-query";
import { Navigate } from "react-router-dom";
import { LoginCard } from "@/components/login/LoginCard";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { useAuthHydration } from "@/hooks/useAuthHydration";
import { usePostLoginRedirect } from "@/hooks/usePostLoginRedirect";
import { resolvePostAuthPath } from "@/lib/authRedirect";
import { fetchTermsLatest } from "@/lib/searchApi";
import { useAuthStore } from "@/stores/authStore";

const emailEnabled =
  import.meta.env.VITE_LOGIN_EMAIL_ENABLED !== "false";
const oauthEnabled =
  import.meta.env.VITE_LOGIN_OAUTH_ENABLED !== "false";

export function LoginPage() {
  const hydrated = useAuthHydration();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const redirectAfterLogin = usePostLoginRedirect();

  const canaryQuery = useQuery({
    queryKey: ["login", "canary"],
    queryFn: async () => {
      const response = await fetchTermsLatest();
      return response.data;
    },
    enabled: hydrated && !isAuthenticated,
  });

  if (hydrated && isAuthenticated && user) {
    return <Navigate to={resolvePostAuthPath(user)} replace />;
  }

  const pageState: PageStateKind = (() => {
    if (!hydrated) return "loading";
    if (canaryQuery.isLoading) return "loading";
    if (canaryQuery.isError) return "error";
    if (!emailEnabled && !oauthEnabled) return "empty";
    return "populated";
  })();

  return (
    <div className="flex min-h-[calc(100vh-4rem-12rem)] flex-1 flex-col items-center justify-center px-4 py-10">
      <PageState
        state={pageState}
        loading={
          <div className="w-full max-w-[440px] space-y-4">
            <SkeletonBlock className="h-10 w-32" />
            <SkeletonBlock className="h-11 w-full" />
            <SkeletonBlock className="h-11 w-full" />
            <SkeletonBlock className="h-11 w-full" />
          </div>
        }
        error={
          <div className="w-full max-w-[440px] rounded-xl border border-warm-border bg-cream p-6 text-center">
            <p className="text-charcoal">서버에 연결할 수 없습니다.</p>
            <p className="mt-2 text-sm text-muted-gray">
              백엔드가 실행 중인지 확인해 주세요. (./init.sh start)
            </p>
            <button
              type="button"
              onClick={() => void canaryQuery.refetch()}
              className="mt-4 rounded-md border border-charcoal/40 px-4 py-2 text-sm text-charcoal hover:opacity-80"
            >
              다시 시도
            </button>
          </div>
        }
        empty={
          <div className="w-full max-w-[440px] rounded-xl border border-warm-border bg-cream p-6 text-center text-muted-gray">
            로그인 방법이 설정되지 않았습니다.
          </div>
        }
      >
        <div className="flex w-full max-w-[440px] flex-col items-center">
          <LoginCard
            emailEnabled={emailEnabled}
            oauthEnabled={oauthEnabled}
            onLoginSuccess={redirectAfterLogin}
          />
          <button
            type="button"
            onClick={() => {
              logout();
              void canaryQuery.refetch();
            }}
            className="mt-3 text-[11px] text-muted-gray/80 underline decoration-muted-gray/40 underline-offset-2 hover:text-charcoal"
          >
            저장된 로그인 정보 지우기
          </button>
        </div>
      </PageState>
    </div>
  );
}
