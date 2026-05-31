import { useState } from "react";
import { ApiRequestError } from "@/lib/apiClient";
import { fetchOAuthUrl } from "@/lib/authApi";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { SocialProviderButton } from "@/components/login/SocialProviderButton";

const providers = [
  { id: "google" as const, label: "Google로 계속하기" },
  { id: "naver" as const, label: "네이버로 계속하기" },
  { id: "kakao" as const, label: "카카오로 계속하기" },
];

interface SocialLoginStackProps {
  disabled?: boolean;
}

export function SocialLoginStack({ disabled = false }: SocialLoginStackProps) {
  const [loadingProvider, setLoadingProvider] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const startOAuth = async (provider: string) => {
    setError(null);
    setLoadingProvider(provider);
    try {
      const response = await fetchOAuthUrl(provider);
      const url = response.data?.authorizationUrl;
      if (!url) {
        throw new ApiRequestError("ERR_AUTH_010", "OAuth URL을 받지 못했습니다.");
      }
      window.location.assign(url);
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      } else if (err instanceof TypeError) {
        setError("네트워크 연결을 확인해 주세요.");
      } else {
        setError("소셜 로그인을 시작할 수 없습니다.");
      }
      setLoadingProvider(null);
    }
  };

  return (
    <div className="space-y-3">
      {error ? (
        <ErrorBanner message={error} onRetry={() => setError(null)} />
      ) : null}

      {providers.map((item) => (
        <SocialProviderButton
          key={item.id}
          provider={item.id}
          label={item.label}
          disabled={disabled || loadingProvider !== null}
          onClick={() => void startOAuth(item.id)}
        />
      ))}
    </div>
  );
}
