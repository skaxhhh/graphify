import { Link } from "react-router-dom";
import { DividerWithLabel } from "@/components/login/DividerWithLabel";
import { EmailLoginForm } from "@/components/login/EmailLoginForm";
import { SocialLoginStack } from "@/components/login/SocialLoginStack";
import type { AuthUser } from "@/types/auth";

interface LoginCardProps {
  emailEnabled: boolean;
  oauthEnabled: boolean;
  onLoginSuccess: (user: AuthUser) => void;
}

export function LoginCard({
  emailEnabled,
  oauthEnabled,
  onLoginSuccess,
}: LoginCardProps) {
  return (
    <div className="w-full max-w-[440px] rounded-xl border border-warm-border bg-cream p-5 md:p-8">
      <h1 className="text-[28px] font-semibold leading-tight text-charcoal md:text-[32px]">
        로그인
      </h1>

      {oauthEnabled ? <div className="mt-6"><SocialLoginStack /></div> : null}

      {emailEnabled && oauthEnabled ? <DividerWithLabel /> : null}

      {emailEnabled ? (
        <div className={oauthEnabled ? "" : "mt-6"}>
          <EmailLoginForm onSuccess={onLoginSuccess} />
        </div>
      ) : null}

      {emailEnabled ? (
        <div className="mt-6">
          <Link
            to="/password-reset"
            className="text-sm text-muted-gray underline decoration-muted-gray/50 underline-offset-2 transition-colors hover:text-charcoal hover:decoration-charcoal"
          >
            비밀번호 찾기
          </Link>
        </div>
      ) : null}
    </div>
  );
}
