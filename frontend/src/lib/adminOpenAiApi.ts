import { apiGet, apiPut } from "@/lib/apiClient";
import type {
  OpenAiConfig,
  OpenAiConfigUpdatePayload,
  OpenAiStatus,
} from "@/types/openaiConfig";

export async function fetchOpenAiConfig() {
  return apiGet<OpenAiConfig>("/api/v1/admin/openai/config");
}

export async function updateOpenAiConfig(payload: OpenAiConfigUpdatePayload) {
  return apiPut<OpenAiConfig, OpenAiConfigUpdatePayload>(
    "/api/v1/admin/openai/config",
    payload
  );
}

export async function fetchOpenAiStatus(refresh = false) {
  const query = refresh ? "?refresh=true" : "";
  return apiGet<OpenAiStatus>(`/api/v1/admin/openai/status${query}`);
}
