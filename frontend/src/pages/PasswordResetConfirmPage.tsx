import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { ResetConfirmCard } from "@/components/password-reset/ResetConfirmCard";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { validatePasswordResetToken } from "@/lib/passwordResetApi";
import { fetchTermsLatest } from "@/lib/searchApi";

export function PasswordResetConfirmPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token")?.trim() ?? "";

  const canaryQuery = useQuery({
    queryKey: ["password-reset-confirm", "canary"],
    queryFn: async () => {
      const response = await fetchTermsLatest();
      return response.data;
    },
  });

  const validateQuery = useQuery({
    queryKey: ["password-reset", "validate", token],
    queryFn: async () => {
      const response = await validatePasswordResetToken(token);
      return response.data;
    },
    enabled: Boolean(token) && canaryQuery.isSuccess,
    retry: false,
  });

  const pageState: PageStateKind = (() => {
    if (!token) return "empty";
    if (canaryQuery.isLoading || validateQuery.isLoading) return "loading";
    if (canaryQuery.isError || validateQuery.isError) return "error";
    return "populated";
  })();

  const tokenValid = validateQuery.data?.valid === true;

  return (
    <div className="flex min-h-[calc(100vh-4rem-12rem)] flex-1 flex-col items-center justify-center px-4 py-12 md:py-20">
      <PageState
        state={pageState}
        loading={
          <div className="w-full max-w-[440px] space-y-4 rounded-xl border border-warm-border bg-cream p-6 md:p-8">
            <SkeletonBlock className="h-8 w-48" />
            <SkeletonBlock className="h-4 w-full" />
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
              onClick={() => {
                void canaryQuery.refetch();
                void validateQuery.refetch();
              }}
              className="mt-4 rounded-md border border-charcoal/40 px-4 py-2 text-sm text-charcoal hover:opacity-80"
            >
              다시 시도
            </button>
          </div>
        }
        empty={
          <div className="w-full max-w-[440px] rounded-xl border border-warm-border bg-cream p-6 text-center md:p-8">
            <p className="text-charcoal">유효하지 않은 접근입니다.</p>
            <p className="mt-2 text-sm text-muted-gray">
              비밀번호 재설정 메일의 링크를 통해 접속해 주세요.
            </p>
            <a
              href="/password-reset"
              className="mt-4 inline-block text-sm text-charcoal underline"
            >
              재설정 메일 요청하기
            </a>
          </div>
        }
      >
        <ResetConfirmCard token={token} tokenValid={tokenValid} />
      </PageState>
    </div>
  );
}
