export type OpenAiConnectionStatus = "OK" | "ERROR" | "NOT_CONFIGURED";

export interface OpenAiConfig {
  configured: boolean;
  endpointUrl: string;
  deploymentName: string;
  apiVersion: string;
  model: string;
  temperature: number;
  maxTokens: number;
  topP: number;
  embeddingModel: string;
  embeddingDeployment: string;
  fallbackEndpoint: string | null;
  fallbackDeploymentName: string | null;
  hasApiKey: boolean;
  hasFallbackApiKey: boolean;
}

export interface OpenAiConfigUpdatePayload {
  endpointUrl: string;
  apiKey?: string;
  deploymentName: string;
  apiVersion: string;
  model: string;
  temperature: number;
  maxTokens: number;
  topP: number;
  embeddingModel: string;
  embeddingDeployment: string;
  fallbackEndpoint?: string;
  fallbackApiKey?: string;
  fallbackDeploymentName?: string;
}

export interface OpenAiStatus {
  connection: OpenAiConnectionStatus;
  tokensUsed: number;
  rateLimitRemaining: number;
  lastCheckedAt: string | null;
  message: string | null;
}

export const OPENAI_MODEL_OPTIONS = [
  { value: "gpt-4o", label: "gpt-4o" },
  { value: "gpt-4o-mini", label: "gpt-4o-mini" },
  { value: "gpt-4.1", label: "gpt-4.1" },
  { value: "gpt-4.1-mini", label: "gpt-4.1-mini" },
] as const;

export const DEFAULT_OPENAI_DRAFT: OpenAiConfigUpdatePayload = {
  endpointUrl: "https://your-resource.openai.azure.com",
  deploymentName: "gpt-4o",
  apiVersion: "2024-02-15",
  model: "gpt-4o",
  temperature: 0.3,
  maxTokens: 4096,
  topP: 1,
  embeddingModel: "text-embedding-3-large",
  embeddingDeployment: "text-embedding-3-large",
  fallbackEndpoint: "",
  fallbackDeploymentName: "",
};
