import { useState } from "react";
import type { FormEvent } from "react";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { PasswordField } from "@/components/shared/PasswordField";
import { PasswordStrengthMeter } from "@/components/shared/PasswordStrengthMeter";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { useDebounce } from "@/hooks/useDebounce";
import { ApiRequestError } from "@/lib/apiClient";
import { evaluatePasswordPolicy } from "@/lib/passwordPolicy";
import { changePassword } from "@/lib/userApi";

interface PasswordChangeFormProps {
  onSuccess?: (message: string) => void;
}

export function PasswordChangeForm({ onSuccess }: PasswordChangeFormProps) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [confirmTouched, setConfirmTouched] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const debouncedPassword = useDebounce(newPassword, 300);
  const policy = evaluatePasswordPolicy(debouncedPassword);

  const mismatch =
    confirmTouched && confirmPassword.length > 0 && newPassword !== confirmPassword;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!policy.meetsPolicy) {
      setError("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("새 비밀번호 확인이 일치하지 않습니다.");
      return;
    }

    setSubmitting(true);
    try {
      const response = await changePassword({ currentPassword, newPassword });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setConfirmTouched(false);
      onSuccess?.(response.data?.message ?? "비밀번호가 변경되었습니다.");
    } catch (err) {
      setError(
        err instanceof ApiRequestError ? err.message : "비밀번호 변경에 실패했습니다."
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="rounded-xl border border-warm-border bg-cream p-6">
      <h2 className="text-base font-semibold text-charcoal">비밀번호 변경</h2>
      <p className="mt-1 text-xs text-muted-gray">8자 이상, 영문·숫자 조합을 권장합니다.</p>

      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        {error ? <ErrorBanner message={error} /> : null}

        <PasswordField
          label="현재 비밀번호"
          autoComplete="current-password"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          disabled={submitting}
        />
        <PasswordField
          label="새 비밀번호"
          autoComplete="new-password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          disabled={submitting}
        />
        {newPassword ? <PasswordStrengthMeter strength={policy.strength} /> : null}
        <PasswordField
          label="새 비밀번호 확인"
          autoComplete="new-password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          onBlur={() => setConfirmTouched(true)}
          error={mismatch ? "비밀번호가 일치하지 않습니다." : undefined}
          disabled={submitting}
        />

        <PrimaryButton type="submit" loading={submitting} className="md:!w-auto md:px-8">
          비밀번호 변경
        </PrimaryButton>
      </form>
    </section>
  );
}
