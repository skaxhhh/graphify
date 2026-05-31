export type AuthProvider = "email" | "google" | "naver" | "kakao";

export type AuthRole = "USER" | "ADMIN";

export interface AuthUser {
  id: number;
  email: string;
  displayName: string;
  termsAccepted: boolean;
  isNewUser: boolean;
  authProvider: AuthProvider;
  role?: AuthRole;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

export interface OAuthUrlResponse {
  authorizationUrl: string;
  state: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ConsentRequest {
  acceptedTermIds: number[];
  version: string;
}

export interface ConsentResponse {
  user: AuthUser;
}
