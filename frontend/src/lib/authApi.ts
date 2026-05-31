import { apiGet, apiPost } from "@/lib/apiClient";
import type {
  ConsentRequest,
  ConsentResponse,
  LoginRequest,
  LoginResponse,
  OAuthUrlResponse,
} from "@/types/auth";

export async function loginWithEmail(payload: LoginRequest) {
  return apiPost<LoginResponse, LoginRequest>("/api/v1/auth/login", payload);
}

export async function fetchOAuthUrl(provider: string) {
  return apiGet<OAuthUrlResponse>(`/api/v1/auth/oauth/${provider}/url`);
}

export async function submitConsent(payload: ConsentRequest) {
  return apiPost<ConsentResponse, ConsentRequest>(
    "/api/v1/auth/consent",
    payload
  );
}

export async function logout() {
  return apiPost<null, Record<string, never>>("/api/v1/auth/logout", {});
}
