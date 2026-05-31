import type { ApiResponse } from "@/types/api";

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "";

const STORAGE_ACCESS = "graphify.accessToken";
const STORAGE_REFRESH = "graphify.refreshToken";

export function getAccessToken(): string | null {
  return localStorage.getItem(STORAGE_ACCESS);
}

function buildHeaders(extra?: HeadersInit): HeadersInit {
  const headers: Record<string, string> = {
    Accept: "application/json",
  };
  const token = getAccessToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (extra) {
    const extraRecord = extra instanceof Headers ? Object.fromEntries(extra.entries()) : extra;
    Object.assign(headers, extraRecord as Record<string, string>);
  }
  return headers;
}

export class ApiRequestError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = "ApiRequestError";
    this.code = code;
  }
}

async function parseJson<T>(response: Response): Promise<T | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }
  return JSON.parse(text) as T;
}

function errorFromStatus(status: number): ApiRequestError {
  if (status === 401) {
    return new ApiRequestError("ERR_AUTH_001", "로그인이 필요합니다. 다시 로그인해 주세요.");
  }
  if (status === 403) {
    return new ApiRequestError("ERR_AUTH_003", "접근 권한이 없습니다.");
  }
  return new ApiRequestError(`ERR_HTTP_${status}`, "서버 응답을 처리할 수 없습니다.");
}

async function request<T>(
  path: string,
  init?: RequestInit
): Promise<ApiResponse<T>> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: buildHeaders(init?.headers),
  });
  const body = await parseJson<ApiResponse<T>>(response);

  if (!body) {
    throw errorFromStatus(response.status);
  }

  if (!response.ok || !body.success) {
    const code = body.error?.code ?? `ERR_HTTP_${response.status}`;
    const message = body.error?.message ?? "요청에 실패했습니다.";
    throw new ApiRequestError(code, message);
  }

  return body;
}

export async function apiGet<T>(path: string): Promise<ApiResponse<T>> {
  return request<T>(path);
}

export async function apiPost<T, B>(
  path: string,
  payload: B
): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export async function apiDelete<T>(path: string): Promise<ApiResponse<T>> {
  return request<T>(path, { method: "DELETE" });
}

export async function apiDeleteWithBody<T, B>(
  path: string,
  payload: B
): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export async function apiPut<T, B>(
  path: string,
  payload: B
): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export const authStorageKeys = {
  accessToken: STORAGE_ACCESS,
  refreshToken: STORAGE_REFRESH,
  user: "graphify.user",
} as const;
