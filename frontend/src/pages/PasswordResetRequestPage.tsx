import { useQuery } from "@tanstack/react-query";
import { ResetRequestCard } from "@/components/password-reset/ResetRequestCard";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { fetchTermsLatest } from "@/lib/searchApi";

const emailLoginEnabled =
  import.meta.env.VITE_LOGIN_EMAIL_ENABLED !== "false";

export function PasswordResetRequestPage() {
  const canaryQuery = useQuery({
    queryKey: ["password-reset", "canary"],
    queryFn: async () => {
      const response = await fetchTermsLatest();
      return response.data;
    },
  });

  const pageState: PageStateKind = (() => {
    if (canaryQuery.isLoading) return "loading";
    if (canaryQuery.isError) return "error";
    if (!emailLoginEnabled) return "empty";
    return "populated";
  })();

  return (
    <div className="flex min-h-[calc(100vh-4rem-12rem)] flex-1 flex-col items-center justify-center px-4 py-12 md:py-20">
      <PageState
        state={pageState}
        loading={
          <div className="w-full max-w-[440px] space-y-4">
            <SkeletonBlock className="h-10 w-40" />
            <SkeletonBlock className="h-4 w-full" />
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
            이메일 로그인이 비활성화되어 비밀번호 재설정을 사용할 수 없습니다.
          </div>
        }
      >
        <ResetRequestCard disabled={!emailLoginEnabled} />
      </PageState>
    </div>
  );
}
