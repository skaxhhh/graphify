import { apiGet, apiPost } from "@/lib/apiClient";
import type {
  PasswordResetConfirmPayload,
  PasswordResetConfirmResponse,
  PasswordResetRequestPayload,
  PasswordResetRequestResponse,
  PasswordResetValidateResponse,
} from "@/types/passwordReset";

export async function requestPasswordReset(payload: PasswordResetRequestPayload) {
  return apiPost<PasswordResetRequestResponse, PasswordResetRequestPayload>(
    "/api/v1/auth/password-reset/request",
    payload
  );
}

export async function validatePasswordResetToken(token: string) {
  const query = new URLSearchParams({ token });
  return apiGet<PasswordResetValidateResponse>(
    `/api/v1/auth/password-reset/validate?${query.toString()}`
  );
}

export async function confirmPasswordReset(payload: PasswordResetConfirmPayload) {
  return apiPost<PasswordResetConfirmResponse, PasswordResetConfirmPayload>(
    "/api/v1/auth/password-reset/confirm",
    payload
  );
}
