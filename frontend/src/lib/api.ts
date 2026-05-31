import type { ActuatorHealth, ApiResponse, BootstrapStatus } from "@/types/api";

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "";

async function parseJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!text) {
    throw new Error("Empty response body");
  }
  return JSON.parse(text) as T;
}

export async function fetchActuatorHealth(): Promise<ActuatorHealth> {
  const response = await fetch(`${baseUrl}/actuator/health`);
  if (!response.ok) {
    throw new Error(`Health check failed: ${response.status}`);
  }
  return parseJson<ActuatorHealth>(response);
}

export async function fetchBootstrapStatus(): Promise<ApiResponse<BootstrapStatus>> {
  const response = await fetch(`${baseUrl}/api/v1/bootstrap/status`);
  return parseJson<ApiResponse<BootstrapStatus>>(response);
}
