import { apiDelete, apiGet, apiPost, apiPut } from "@/lib/apiClient";
import type {
  McpConnectionStatus,
  McpTool,
  McpToolListData,
  McpToolPingResult,
  McpToolUpsertPayload,
} from "@/types/mcpTool";

export interface McpToolListParams {
  q?: string;
  status?: McpConnectionStatus | "ALL";
}

export async function fetchMcpTools(params: McpToolListParams = {}) {
  const search = new URLSearchParams();
  if (params.q) search.set("q", params.q);
  if (params.status && params.status !== "ALL") search.set("status", params.status);
  const query = search.toString();
  return apiGet<McpToolListData>(
    `/api/v1/admin/tools${query ? `?${query}` : ""}`
  );
}

export async function createMcpTool(payload: McpToolUpsertPayload) {
  return apiPost<McpTool, McpToolUpsertPayload>("/api/v1/admin/tools", payload);
}

export async function updateMcpTool(id: number, payload: McpToolUpsertPayload) {
  return apiPut<McpTool, McpToolUpsertPayload>(
    `/api/v1/admin/tools/${id}`,
    payload
  );
}

export async function deleteMcpTool(id: number) {
  return apiDelete<void>(`/api/v1/admin/tools/${id}`);
}

export async function pingMcpTool(id: number) {
  return apiPost<McpToolPingResult, Record<string, never>>(
    `/api/v1/admin/tools/${id}/ping`,
    {}
  );
}
