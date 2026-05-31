import type { AuthProvider } from "@/types/auth";

export interface UserMe {
  id: number;
  email: string;
  displayName: string;
  authProvider: AuthProvider;
  isPremium: boolean;
  customPrompt: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UpdatePromptRequest {
  customPrompt: string;
}

export interface MessageResponse {
  message: string;
}
