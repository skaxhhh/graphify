export interface PasswordResetRequestPayload {
  email: string;
}

export interface PasswordResetRequestResponse {
  message: string;
  maskedEmail: string;
}

export interface PasswordResetValidateResponse {
  valid: boolean;
  expiresAt: string | null;
}

export interface PasswordResetConfirmPayload {
  token: string;
  newPassword: string;
}

export interface PasswordResetConfirmResponse {
  message: string;
}
