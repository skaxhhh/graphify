import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { useAuthStore } from "@/stores/authStore";

interface RequireAuthProps {
  children: ReactNode;
}

export function RequireAuth({ children }: RequireAuthProps) {
  const location = useLocation();
  const hydrated = useAuthStore((s) => s.hydrated);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  if (!hydrated) {
    return (
      <div className="space-y-4 p-8">
        <SkeletonBlock className="h-8 w-48" />
        <SkeletonBlock className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname + location.search }}
      />
    );
  }

  return <>{children}</>;
}
