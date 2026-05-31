import { useState } from "react";
import type { FormEvent } from "react";
import { ApiRequestError } from "@/lib/apiClient";
import { loginWithEmail } from "@/lib/authApi";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { InlineError } from "@/components/shared/InlineError";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { TextField } from "@/components/shared/TextField";
import { useAuthStore } from "@/stores/authStore";
import type { AuthUser } from "@/types/auth";

interface EmailLoginFormProps {
  onSuccess: (user: AuthUser) => void;
  disabled?: boolean;
}

export function EmailLoginForm({ onSuccess, disabled = false }: EmailLoginFormProps) {
  const setSession = useAuthStore((s) => s.setSession);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [networkError, setNetworkError] = useState<string | null>(null);
  const [inlineError, setInlineError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>(
    {}
  );

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setNetworkError(null);
    setInlineError(null);
    setFieldErrors({});

    const nextErrors: { email?: string; password?: string } = {};
    if (!email.trim()) nextErrors.email = "이메일을 입력해 주세요.";
    if (!password) nextErrors.password = "비밀번호를 입력해 주세요.";
    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(nextErrors);
      return;
    }

    setSubmitting(true);
    try {
      const response = await loginWithEmail({ email: email.trim(), password });
      if (!response.data) {
        throw new ApiRequestError("ERR_AUTH_000", "로그인 응답이 비어 있습니다.");
      }
      setSession(response.data);
      onSuccess(response.data.user);
    } catch (error) {
      if (error instanceof ApiRequestError) {
        if (error.code === "ERR_AUTH_001" || error.code.startsWith("ERR_VALIDATION")) {
          setInlineError(error.message);
        } else {
          setNetworkError(error.message);
        }
      } else if (error instanceof TypeError) {
        setNetworkError("네트워크 연결을 확인해 주세요.");
      } else {
        setNetworkError("로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
      {networkError ? (
        <ErrorBanner message={networkError} onRetry={() => setNetworkError(null)} />
      ) : null}

      <TextField
        label="이메일"
        type="email"
        autoComplete="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="you@example.com"
        disabled={disabled || submitting}
        error={fieldErrors.email}
      />
      <TextField
        label="비밀번호"
        type="password"
        autoComplete="current-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        disabled={disabled || submitting}
        error={fieldErrors.password}
      />

      {inlineError ? <InlineError message={inlineError} /> : null}

      <PrimaryButton type="submit" loading={submitting} disabled={disabled}>
        로그인
      </PrimaryButton>
    </form>
  );
}
