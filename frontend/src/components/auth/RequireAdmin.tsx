import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { useAuthStore } from "@/stores/authStore";

interface RequireAdminProps {
  children: ReactNode;
}

export function RequireAdmin({ children }: RequireAdminProps) {
  const hydrated = useAuthStore((s) => s.hydrated);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const role = useAuthStore((s) => s.role);

  if (!hydrated) {
    return (
      <div className="space-y-4 p-8">
        <SkeletonBlock className="h-8 w-48" />
        <SkeletonBlock className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (role !== "admin") {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
