import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { AccountActions } from "@/components/mypage/AccountActions";
import { MyPageSkeleton } from "@/components/mypage/MyPageSkeleton";
import { PremiumPromptSection } from "@/components/mypage/PremiumPromptSection";
import { ProfileSummaryCard } from "@/components/mypage/ProfileSummaryCard";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { LogoutConfirmDialog } from "@/components/shared/LogoutConfirmDialog";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { PasswordChangeForm } from "@/components/shared/PasswordChangeForm";
import { EmptyState } from "@/components/shared/EmptyState";
import { ApiRequestError } from "@/lib/apiClient";
import { logout } from "@/lib/authApi";
import { fetchUserMe, updateCustomPrompt } from "@/lib/userApi";
import { useAuthStore } from "@/stores/authStore";

export function MyPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const authLogout = useAuthStore((s) => s.logout);

  const [logoutOpen, setLogoutOpen] = useState(false);
  const [promptDraft, setPromptDraft] = useState("");
  const [toast, setToast] = useState<string | null>(null);

  const meQuery = useQuery({
    queryKey: ["users", "me"],
    queryFn: async () => {
      const response = await fetchUserMe();
      return response.data;
    },
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  useEffect(() => {
    if (meQuery.data?.customPrompt != null) {
      setPromptDraft(meQuery.data.customPrompt);
    }
  }, [meQuery.data?.customPrompt]);

  useEffect(() => {
    if (
      meQuery.error instanceof ApiRequestError &&
      meQuery.error.code.startsWith("ERR_AUTH")
    ) {
      authLogout();
    }
  }, [meQuery.error, authLogout]);

  const promptMutation = useMutation({
    mutationFn: () => updateCustomPrompt({ customPrompt: promptDraft }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["users", "me"] });
      setToast(response.data?.message ?? "프롬프트가 저장되었습니다.");
      window.setTimeout(() => setToast(null), 2000);
    },
  });

  const logoutMutation = useMutation({
    mutationFn: async () => {
      try {
        await logout();
      } finally {
        authLogout();
      }
    },
    onSuccess: () => {
      setLogoutOpen(false);
      navigate("/login", { replace: true });
    },
  });

  const user = meQuery.data;

  const pageState: PageStateKind = (() => {
    if (meQuery.isLoading) return "loading";
    if (meQuery.isError) return "error";
    if (!user) return "empty";
    return "populated";
  })();

  const handlePasswordSuccess = (message: string) => {
    setToast(message);
    window.setTimeout(() => {
      setToast(null);
      authLogout();
      navigate("/login", { replace: true });
    }, 2000);
  };

  return (
    <>
      {toast ? (
        <div
          className="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-lg bg-charcoal px-4 py-2 text-sm text-off-white shadow-lg"
          role="status"
        >
          {toast}
        </div>
      ) : null}

      <PageState
        state={pageState}
        loading={<MyPageSkeleton />}
        empty={
          <div className="mx-auto max-w-[720px] px-4 py-10 md:px-8">
            <EmptyState
              title="프로필 정보를 찾을 수 없습니다"
              description="다시 로그인해 주세요."
            />
          </div>
        }
        error={
          <div className="mx-auto max-w-[720px] px-4 py-10 md:px-8">
            <ErrorBanner
              message={
                meQuery.error instanceof Error
                  ? meQuery.error.message
                  : "프로필을 불러오지 못했습니다."
              }
              onRetry={() => meQuery.refetch()}
            />
          </div>
        }
      >
        {user ? (
          <div className="mx-auto flex max-w-[720px] flex-col gap-6 px-4 py-10 md:gap-8 md:px-8">
            <ProfileSummaryCard user={user} />

            {user.authProvider === "email" ? (
              <PasswordChangeForm onSuccess={handlePasswordSuccess} />
            ) : null}

            {user.isPremium ? (
              <PremiumPromptSection
                customPrompt={promptDraft}
                onChange={setPromptDraft}
                onSave={() => promptMutation.mutate()}
                saving={promptMutation.isPending}
              />
            ) : null}

            <AccountActions onLogoutClick={() => setLogoutOpen(true)} />
          </div>
        ) : null}
      </PageState>

      <LogoutConfirmDialog
        open={logoutOpen}
        onClose={() => setLogoutOpen(false)}
        onConfirm={() => logoutMutation.mutate()}
        loading={logoutMutation.isPending}
      />
    </>
  );
}
