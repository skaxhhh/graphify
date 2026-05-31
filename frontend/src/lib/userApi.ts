import { apiGet, apiPut } from "@/lib/apiClient";
import type {
  ChangePasswordRequest,
  MessageResponse,
  UpdatePromptRequest,
  UserMe,
} from "@/types/user";

export async function fetchUserMe() {
  return apiGet<UserMe>("/api/v1/users/me");
}

export async function changePassword(payload: ChangePasswordRequest) {
  return apiPut<MessageResponse, ChangePasswordRequest>(
    "/api/v1/users/me/password",
    payload
  );
}

export async function updateCustomPrompt(payload: UpdatePromptRequest) {
  return apiPut<MessageResponse, UpdatePromptRequest>(
    "/api/v1/users/me/prompt",
    payload
  );
}
