export type McpAuthType = "NONE" | "API_KEY" | "BEARER";

export type McpConnectionStatus =
  | "CONNECTED"
  | "DISCONNECTED"
  | "ERROR"
  | "UNKNOWN";

export type McpRole = "USER" | "ADMIN" | "PREMIUM";

export interface McpTool {
  id: number;
  name: string;
  description: string;
  endpointUrl: string;
  authType: McpAuthType;
  schemaJson: string | null;
  status: McpConnectionStatus;
  enabled: boolean;
  allowedRoles: McpRole[];
  lastCalledAt: string | null;
}

export interface McpToolListData {
  tools: McpTool[];
}

export interface McpToolUpsertPayload {
  name: string;
  description?: string;
  endpointUrl: string;
  authType: McpAuthType;
  authSecret?: string;
  schemaJson?: string;
  enabled?: boolean;
  allowedRoles: McpRole[];
}

export interface McpToolPingResult {
  ok: boolean;
  latencyMs: number;
  message: string;
}
