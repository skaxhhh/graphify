import { Outlet, useLocation } from "react-router-dom";
import { AppHeaderGuest } from "@/components/shared/AppHeaderGuest";
import { MarketingFooter } from "@/components/shared/MarketingFooter";

/** SCREEN_FLOW §3-1 Guest Layout */
export function GuestLayout() {
  const { pathname } = useLocation();
  const isGraphRoute = pathname.includes("/graph");

  return (
    <div className="flex h-screen flex-col overflow-hidden">
      <AppHeaderGuest />
      <main
        className={
          isGraphRoute
            ? "flex min-h-0 flex-1 flex-col overflow-hidden"
            : "flex min-h-0 flex-1 flex-col overflow-y-auto"
        }
      >
        <Outlet />
      </main>
      {isGraphRoute ? null : <MarketingFooter />}
    </div>
  );
}
