import { Outlet } from "react-router-dom";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { AppHeaderUser } from "@/components/shared/AppHeaderUser";
import { UserFooterSlim } from "@/components/shared/UserFooterSlim";

/** SCREEN_FLOW §3-2 User App Layout */
export function UserAppLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <AppHeaderUser />
      <main className="flex flex-1 flex-col">
        <RequireAuth>
          <Outlet />
        </RequireAuth>
      </main>
      <UserFooterSlim />
    </div>
  );
}
