import { create } from "zustand";
import { authStorageKeys } from "@/lib/apiClient";
import type { AuthProvider, AuthUser, LoginResponse } from "@/types/auth";

export type UserRole = "guest" | "user" | "admin";

interface AuthState {
  role: UserRole;
  isAuthenticated: boolean;
  hydrated: boolean;
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  hydrate: () => void;
  setSession: (response: LoginResponse) => void;
  updateUser: (user: AuthUser) => void;
  setRole: (role: UserRole) => void;
  logout: () => void;
}

function roleFromUser(user: AuthUser | null): UserRole {
  if (!user) return "guest";
  if (user.role === "ADMIN") return "admin";
  return "user";
}

function persistSession(response: LoginResponse) {
  localStorage.setItem(authStorageKeys.accessToken, response.accessToken);
  localStorage.setItem(authStorageKeys.refreshToken, response.refreshToken);
  localStorage.setItem(authStorageKeys.user, JSON.stringify(response.user));
}

function clearSession() {
  localStorage.removeItem(authStorageKeys.accessToken);
  localStorage.removeItem(authStorageKeys.refreshToken);
  localStorage.removeItem(authStorageKeys.user);
}

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(authStorageKeys.user);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  role: "guest",
  isAuthenticated: false,
  hydrated: false,
  accessToken: null,
  refreshToken: null,
  user: null,
  hydrate: () => {
    const accessToken = localStorage.getItem(authStorageKeys.accessToken);
    const refreshToken = localStorage.getItem(authStorageKeys.refreshToken);
    const user = readStoredUser();
    const authenticated = Boolean(accessToken && user);
    set({
      accessToken,
      refreshToken,
      user,
      role: authenticated ? roleFromUser(user) : "guest",
      isAuthenticated: authenticated,
      hydrated: true,
    });
  },
  setSession: (response) => {
    persistSession(response);
    set({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      user: response.user,
      role: roleFromUser(response.user),
      isAuthenticated: true,
      hydrated: true,
    });
  },
  updateUser: (user) => {
    const accessToken = localStorage.getItem(authStorageKeys.accessToken);
    const refreshToken = localStorage.getItem(authStorageKeys.refreshToken);
    if (accessToken && refreshToken) {
      const session: LoginResponse = { accessToken, refreshToken, user };
      persistSession(session);
    } else {
      localStorage.setItem(authStorageKeys.user, JSON.stringify(user));
    }
    set({
      user,
      role: roleFromUser(user),
      isAuthenticated: true,
      hydrated: true,
    });
  },
  setRole: (role) =>
    set({
      role,
      isAuthenticated: role === "user" || role === "admin",
    }),
  logout: () => {
    clearSession();
    set({
      role: "guest",
      isAuthenticated: false,
      accessToken: null,
      refreshToken: null,
      user: null,
      hydrated: true,
    });
  },
}));

export function parseAuthProvider(value: string | null): AuthProvider {
  if (
    value === "email" ||
    value === "google" ||
    value === "naver" ||
    value === "kakao"
  ) {
    return value;
  }
  return "email";
}
