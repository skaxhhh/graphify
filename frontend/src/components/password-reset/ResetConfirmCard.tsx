import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { GhostButton } from "@/components/shared/GhostButton";
import { PasswordField } from "@/components/shared/PasswordField";
import { PasswordStrengthMeter } from "@/components/shared/PasswordStrengthMeter";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { useDebounce } from "@/hooks/useDebounce";
import { ApiRequestError } from "@/lib/apiClient";
import { evaluatePasswordPolicy } from "@/lib/passwordPolicy";
import { confirmPasswordReset } from "@/lib/passwordResetApi";

interface ResetConfirmCardProps {
  token: string;
  tokenValid: boolean;
}

export function ResetConfirmCard({ token, tokenValid }: ResetConfirmCardProps) {
  const navigate = useNavigate();
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [confirmTouched, setConfirmTouched] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [networkError, setNetworkError] = useState<string | null>(null);
  const [policyError, setPolicyError] = useState<string | null>(null);
  const [mismatchError, setMismatchError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [redirectSeconds, setRedirectSeconds] = useState(2);

  const debouncedPassword = useDebounce(newPassword, 300);
  const policy = evaluatePasswordPolicy(debouncedPassword);

  useEffect(() => {
    if (!successMessage) {
      return;
    }
    if (redirectSeconds <= 0) {
      navigate("/login", { replace: true });
      return;
    }
    const timer = window.setTimeout(() => {
      setRedirectSeconds((prev) => prev - 1);
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [successMessage, redirectSeconds, navigate]);

  if (!tokenValid) {
    return (
      <div className="w-full max-w-[440px] rounded-xl border border-warm-border bg-cream p-5 md:p-8">
        <h1 className="text-[28px] font-semibold leading-tight text-charcoal md:text-[32px]">
          링크가 만료되었습니다
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-muted-gray">
          비밀번호 재설정 링크가 만료되었거나 이미 사용되었습니다. 새 링크를 요청해 주세요.
        </p>
        <Link to="/password-reset" className="mt-6 block">
          <GhostButton type="button" fullWidth>
            재설정 메일 다시 받기
          </GhostButton>
        </Link>
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

  const handleConfirmBlur = () => {
    setConfirmTouched(true);
    if (confirmPassword && newPassword !== confirmPassword) {
      setMismatchError("비밀번호가 일치하지 않습니다.");
    } else {
      setMismatchError(null);
    }
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setNetworkError(null);
    setPolicyError(null);
    setMismatchError(null);

    if (!policy.meetsPolicy) {
      setPolicyError("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setMismatchError("비밀번호가 일치하지 않습니다.");
      return;
    }

    setSubmitting(true);
    try {
      const response = await confirmPasswordReset({
        token,
        newPassword,
      });
      if (!response.data) {
        throw new ApiRequestError("ERR_AUTH_000", "응답이 비어 있습니다.");
      }
      setSuccessMessage(response.data.message);
    } catch (error) {
      if (error instanceof ApiRequestError) {
        if (error.code === "ERR_AUTH_012") {
          navigate("/password-reset", { replace: true });
          return;
        }
        if (error.code.startsWith("ERR_VALIDATION")) {
          setPolicyError(error.message);
        } else {
          setNetworkError(error.message);
        }
      } else if (error instanceof TypeError) {
        setNetworkError("네트워크 연결을 확인해 주세요.");
      } else {
        setNetworkError("비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해 주세요.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (successMessage) {
    return (
      <div className="w-full max-w-[440px] rounded-xl border border-warm-border bg-cream p-5 text-center md:p-8">
        <h1 className="text-xl font-semibold text-charcoal">비밀번호 변경 완료</h1>
        <p className="mt-3 text-sm text-muted-gray">{successMessage}</p>
        <p className="mt-4 text-sm text-charcoal">
          {redirectSeconds}초 후 로그인 화면으로 이동합니다.
        </p>
        <PrimaryButton
          type="button"
          className="mt-6"
          onClick={() => navigate("/login", { replace: true })}
        >
          지금 로그인하기
        </PrimaryButton>
      </div>
    );
  }

  return (
    <div className="w-full max-w-[440px] rounded-xl border border-warm-border bg-cream p-5 md:p-8">
      <h1 className="text-[28px] font-semibold leading-tight text-charcoal md:text-[32px]">
        새 비밀번호 설정
      </h1>

      {networkError ? (
        <div className="mt-4">
          <ErrorBanner message={networkError} onRetry={() => setNetworkError(null)} />
        </div>
      ) : null}

      <form onSubmit={(e) => void handleSubmit(e)} className="mt-6 space-y-4">
        <PasswordField
          label="새 비밀번호"
          value={newPassword}
          onChange={(e) => {
            setNewPassword(e.target.value);
            setPolicyError(null);
          }}
          disabled={submitting}
          hint={policy.hint}
          error={policyError ?? undefined}
          autoComplete="new-password"
        />

        <PasswordStrengthMeter strength={policy.strength} />

        <PasswordField
          label="비밀번호 확인"
          value={confirmPassword}
          onChange={(e) => {
            setConfirmPassword(e.target.value);
            if (mismatchError) {
              setMismatchError(null);
            }
          }}
          onBlur={handleConfirmBlur}
          disabled={submitting}
          error={
            confirmTouched && mismatchError ? mismatchError : undefined
          }
          autoComplete="new-password"
        />

        <PrimaryButton
          type="submit"
          className="mt-2 w-full"
          loading={submitting}
          disabled={submitting}
        >
          비밀번호 변경
        </PrimaryButton>
      </form>

      <Link to="/password-reset" className="mt-3 block">
        <GhostButton type="button" fullWidth>
          재설정 메일 다시 받기
        </GhostButton>
      </Link>
    </div>
  );
}
