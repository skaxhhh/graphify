import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PageState } from "@/components/shared/PageState";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { usePostLoginRedirect } from "@/hooks/usePostLoginRedirect";
import { parseAuthProvider, useAuthStore } from "@/stores/authStore";
import type { AuthUser, LoginResponse } from "@/types/auth";

function readHashParams(): URLSearchParams {
  const hash = window.location.hash.startsWith("#")
    ? window.location.hash.slice(1)
    : window.location.hash;
  return new URLSearchParams(hash);
}

export function AuthCallbackPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const redirectAfterLogin = usePostLoginRedirect();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const params = readHashParams();
    const accessToken = params.get("accessToken");
    const refreshToken = params.get("refreshToken");
    const userId = params.get("userId");

    if (!accessToken || !refreshToken || !userId) {
      setError("OAuth 로그인 정보가 없습니다.");
      return;
    }

    const user: AuthUser = {
      id: Number(userId),
      email: decodeURIComponent(params.get("email") ?? ""),
      displayName: decodeURIComponent(
        params.get("displayName") ?? "OAuth 사용자"
      ),
      termsAccepted: params.get("termsAccepted") === "true",
      isNewUser: params.get("isNewUser") === "true",
      authProvider: parseAuthProvider(params.get("authProvider")),
      role: params.get("role") === "ADMIN" ? "ADMIN" : "USER",
    };

    const session: LoginResponse = {
      accessToken,
      refreshToken,
      user,
    };

    setSession(session);
    window.history.replaceState(null, "", window.location.pathname);
    redirectAfterLogin(user);
  }, [redirectAfterLogin, setSession]);

  if (error) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center px-4">
        <p className="text-charcoal">{error}</p>
        <button
          type="button"
          onClick={() => navigate("/login", { replace: true })}
          className="mt-4 text-sm underline text-muted-gray"
        >
          로그인으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <PageState
      state="loading"
      loading={
        <div className="mx-auto w-full max-w-[440px] space-y-4 p-8">
          <SkeletonBlock className="h-8 w-40" />
          <SkeletonBlock className="h-4 w-full" />
          <p className="text-center text-sm text-muted-gray">로그인 처리 중...</p>
        </div>
      }
    >
      {null}
    </PageState>
  );
}
