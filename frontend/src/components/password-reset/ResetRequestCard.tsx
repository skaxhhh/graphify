import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { SuccessNotice } from "@/components/password-reset/SuccessNotice";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { GhostButton } from "@/components/shared/GhostButton";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { TextField } from "@/components/shared/TextField";
import { ApiRequestError } from "@/lib/apiClient";
import { maskEmail } from "@/lib/maskEmail";
import { requestPasswordReset } from "@/lib/passwordResetApi";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface ResetRequestCardProps {
  disabled?: boolean;
}

export function ResetRequestCard({ disabled = false }: ResetRequestCardProps) {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [networkError, setNetworkError] = useState<string | null>(null);
  const [rateLimitMessage, setRateLimitMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState<{
    message: string;
    maskedEmail: string;
  } | null>(null);

  const validateEmail = (value: string): string | null => {
    const trimmed = value.trim();
    if (!trimmed) {
      return "이메일을 입력해 주세요.";
    }
    if (!EMAIL_PATTERN.test(trimmed)) {
      return "올바른 이메일 형식이 아닙니다.";
    }
    return null;
  };

  const handleBlur = () => {
    if (!email.trim() || success) {
      return;
    }
    setFieldError(validateEmail(email));
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setNetworkError(null);
    setRateLimitMessage(null);

    const validationError = validateEmail(email);
    if (validationError) {
      setFieldError(validationError);
      return;
    }

    setFieldError(null);
    setSubmitting(true);
    try {
      const response = await requestPasswordReset({ email: email.trim() });
      if (!response.data) {
        throw new ApiRequestError("ERR_AUTH_000", "응답이 비어 있습니다.");
      }
      setSuccess({
        message: response.data.message,
        maskedEmail: response.data.maskedEmail || maskEmail(email),
      });
    } catch (error) {
      if (error instanceof ApiRequestError) {
        if (error.code === "ERR_AUTH_011") {
          setRateLimitMessage(error.message);
        } else if (
          error.code.startsWith("ERR_VALIDATION") ||
          error.code === "ERR_AUTH_001"
        ) {
          setFieldError(error.message);
        } else {
          setNetworkError(error.message);
        }
      } else if (error instanceof TypeError) {
        setNetworkError("네트워크 연결을 확인해 주세요.");
      } else {
        setNetworkError("요청에 실패했습니다. 잠시 후 다시 시도해 주세요.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="w-full max-w-[440px] rounded-xl border border-warm-border bg-cream p-5 md:p-8">
      <h1 className="text-[28px] font-semibold leading-tight text-charcoal md:text-[32px]">
        비밀번호 재설정
      </h1>
      <p className="mt-3 text-sm leading-relaxed text-muted-gray">
        가입 시 사용한 이메일을 입력하시면 비밀번호 재설정 안내 메일을 보내 드립니다.
      </p>

      {networkError ? (
        <div className="mt-4">
          <ErrorBanner message={networkError} onRetry={() => setNetworkError(null)} />
        </div>
      ) : null}

      {rateLimitMessage ? (
        <p className="mt-4 text-sm text-muted-gray" role="alert">
          {rateLimitMessage}
        </p>
      ) : null}

      {success ? (
        <div className="mt-6">
          <SuccessNotice message={success.message} maskedEmail={success.maskedEmail} />
        </div>
      ) : (
        <form onSubmit={(e) => void handleSubmit(e)} className="mt-6 space-y-4">
          <TextField
            label="이메일"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (fieldError) {
                setFieldError(null);
              }
            }}
            onBlur={handleBlur}
            placeholder="you@example.com"
            disabled={disabled || submitting}
            error={fieldError ?? undefined}
          />

          <PrimaryButton
            type="submit"
            className="mt-2 w-full"
            loading={submitting}
            disabled={disabled || submitting}
          >
            재설정 메일 보내기
          </PrimaryButton>
        </form>
      )}

      <GhostButton
        type="button"
        fullWidth
        className="mt-3"
        onClick={() => navigate("/login")}
      >
        로그인으로 돌아가기
      </GhostButton>
    </div>
  );
}
