import type { AuthUser } from "@/types/auth";

/** 약관 동의가 필요한지 (백엔드 isNewUser와 termsAccepted 기준) */
export function needsTermsConsent(user: AuthUser): boolean {
  return user.termsAccepted !== true || user.isNewUser === true;
}

/** 로그인·약관 동의 후 이동 경로 */
export function resolvePostAuthPath(user: AuthUser): "/" | "/terms" | "/admin" {
  if (needsTermsConsent(user)) return "/terms";
  if (user.role === "ADMIN") return "/admin";
  return "/";
}
