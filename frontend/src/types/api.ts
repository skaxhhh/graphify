export interface ApiErrorBody {
  code: string;
  message: string;
}

export interface ApiMeta {
  page?: number;
  size?: number;
  total?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  meta: ApiMeta | null;
  error: ApiErrorBody | null;
}

export interface ActuatorHealth {
  status: string;
}

export interface BootstrapStatus {
  service: string;
  phase: string;
}
