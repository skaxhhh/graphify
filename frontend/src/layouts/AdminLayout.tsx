import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";

const navItems = [
  { to: "/admin", label: "대시보드", end: true as const },
  { to: "/admin/users", label: "유저 관리" },
  { to: "/admin/mcp", label: "MCP 도구" },
  { to: "/admin/prompts", label: "프롬프트" },
  { to: "/admin/openai", label: "OpenAI 설정" },
  { to: "/admin/vectordb", label: "Vector DB" },
];

function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav className="flex flex-1 flex-col gap-1 p-3">
      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end ?? false}
          onClick={onNavigate}
          className={({ isActive }) =>
            `rounded-md px-3 py-2 text-sm transition-colors ${
              isActive
                ? "bg-charcoal/5 font-medium text-charcoal"
                : "text-muted-gray hover:bg-charcoal/[0.03] hover:text-charcoal"
            }`
          }
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}

/** SCREEN_FLOW §3-3 Admin Layout */
export function AdminLayout() {
  const user = useAuthStore((s) => s.user);
  const [drawerOpen, setDrawerOpen] = useState(false);

  return (
    <div className="flex min-h-screen bg-cream">
      <aside className="hidden w-64 shrink-0 border-r border-warm-border bg-cream lg:flex lg:flex-col">
        <div className="border-b border-warm-border p-4">
          <p className="text-lg font-semibold text-charcoal">graphify</p>
          <span className="mt-1 inline-block rounded-md border border-charcoal/40 px-2 py-0.5 text-xs text-muted-gray">
            관리자 모드
          </span>
        </div>
        <SidebarNav />
        <div className="border-t border-warm-border p-3">
          <NavLink to="/" className="text-sm text-muted-gray underline">
            사용자 서비스로
          </NavLink>
        </div>
      </aside>

      {drawerOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-charcoal/30"
            aria-label="메뉴 닫기"
            onClick={() => setDrawerOpen(false)}
          />
          <aside className="relative flex h-full w-64 flex-col border-r border-warm-border bg-cream shadow-xl">
            <div className="border-b border-warm-border p-4">
              <p className="text-lg font-semibold text-charcoal">graphify</p>
              <span className="mt-1 inline-block rounded-md border border-charcoal/40 px-2 py-0.5 text-xs text-muted-gray">
                관리자 모드
              </span>
            </div>
            <SidebarNav onNavigate={() => setDrawerOpen(false)} />
            <div className="border-t border-warm-border p-3">
              <NavLink
                to="/"
                className="text-sm text-muted-gray underline"
                onClick={() => setDrawerOpen(false)}
              >
                사용자 서비스로
              </NavLink>
            </div>
          </aside>
        </div>
      ) : null}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-warm-border px-4 md:px-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="rounded-md border border-warm-border px-2 py-1 text-sm text-charcoal lg:hidden"
              onClick={() => setDrawerOpen(true)}
              aria-label="메뉴 열기"
            >
              ☰
            </button>
            <h1 className="text-sm font-medium text-charcoal">관리자</h1>
            <span className="rounded-md border border-charcoal/30 px-2 py-0.5 text-xs text-muted-gray">
              Admin
            </span>
          </div>
          <div className="flex items-center gap-3 text-sm text-muted-gray">
            <NavLink to="/" className="hidden underline sm:inline">
              사용자 서비스로
            </NavLink>
            <span className="max-w-[160px] truncate text-charcoal">
              {user?.displayName ?? "관리자"}
            </span>
          </div>
        </header>
        <main className="flex-1 overflow-auto p-4 md:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
