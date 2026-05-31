import { apiGet, apiPost } from "@/lib/apiClient";
import type {
  AgentPromptDetail,
  AgentPromptRollbackPayload,
  AgentPromptSavePayload,
  AgentPromptTestPayload,
  AgentPromptTestResult,
  PromptTaskType,
} from "@/types/agentPrompt";

export async function fetchAdminPrompt(type: PromptTaskType) {
  return apiGet<AgentPromptDetail>(
    `/api/v1/admin/prompts?type=${encodeURIComponent(type)}`
  );
}

export async function saveAdminPrompt(payload: AgentPromptSavePayload) {
  return apiPost<AgentPromptDetail, AgentPromptSavePayload>(
    "/api/v1/admin/prompts",
    payload
  );
}

export async function rollbackAdminPrompt(
  promptId: number,
  payload: AgentPromptRollbackPayload
) {
  return apiPost<AgentPromptDetail, AgentPromptRollbackPayload>(
    `/api/v1/admin/prompts/${promptId}/rollback`,
    payload
  );
}

export async function testAdminPrompt(
  promptId: number,
  payload: AgentPromptTestPayload
) {
  return apiPost<AgentPromptTestResult, AgentPromptTestPayload>(
    `/api/v1/admin/prompts/${promptId}/test`,
    payload
  );
}
