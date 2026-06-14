import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useTradingStore } from "@/stores/tradingStore";

const navItems = [
  { to: "/trading", label: "DDS Agent", end: true as const },
  { to: "/trading/dashboard", label: "대시보드" },
  { to: "/trading/history", label: "거래 이력" },
  { to: "/trading/rules", label: "현재 룰" },
  { to: "/trading/rules/edit", label: "룰 수정" },
  { to: "/trading/monitor", label: "동작 모니터링" },
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
                ? "bg-white/10 font-medium text-white"
                : "text-gray-400 hover:bg-white/5 hover:text-white"
            }`
          }
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}

export function TradingLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const navigate = useNavigate();
  const disableDarkMode = useTradingStore((s) => s.disableDarkMode);

  const handleExit = () => {
    disableDarkMode();
    navigate("/");
  };

  return (
    <div className="flex min-h-screen bg-gray-950 text-white">
      {/* 데스크탑 사이드바 */}
      <aside className="hidden w-64 shrink-0 border-r border-white/10 bg-gray-900 lg:flex lg:flex-col">
        <div className="border-b border-white/10 p-4">
          <p className="text-lg font-semibold text-white">graphify</p>
          <span className="mt-1 inline-block rounded-md border border-emerald-500/40 px-2 py-0.5 text-xs text-emerald-400">
            트레이딩 봇
          </span>
        </div>
        <SidebarNav />
        <div className="border-t border-white/10 p-3">
          <button
            type="button"
            onClick={handleExit}
            className="text-sm text-gray-500 underline hover:text-gray-300"
          >
            메인으로
          </button>
        </div>
      </aside>

      {/* 모바일 드로어 */}
      {drawerOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-black/60"
            aria-label="메뉴 닫기"
            onClick={() => setDrawerOpen(false)}
          />
          <aside className="relative flex h-full w-64 flex-col border-r border-white/10 bg-gray-900 shadow-xl">
            <div className="border-b border-white/10 p-4">
              <p className="text-lg font-semibold text-white">graphify</p>
              <span className="mt-1 inline-block rounded-md border border-emerald-500/40 px-2 py-0.5 text-xs text-emerald-400">
                트레이딩 봇
              </span>
            </div>
            <SidebarNav onNavigate={() => setDrawerOpen(false)} />
            <div className="border-t border-white/10 p-3">
              <button
                type="button"
                onClick={handleExit}
                className="text-sm text-gray-500 underline hover:text-gray-300"
              >
                메인으로
              </button>
            </div>
          </aside>
        </div>
      ) : null}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-white/10 bg-gray-900 px-4 md:px-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="rounded-md border border-white/20 px-2 py-1 text-sm text-gray-300 lg:hidden"
              onClick={() => setDrawerOpen(true)}
              aria-label="메뉴 열기"
            >
              ☰
            </button>
            <h1 className="text-sm font-medium text-white">자동매매 봇</h1>
            <span className="rounded-md border border-emerald-500/30 px-2 py-0.5 text-xs text-emerald-400">
              BETA
            </span>
          </div>
          <button
            type="button"
            onClick={handleExit}
            className="hidden text-sm text-gray-500 underline hover:text-gray-300 sm:inline"
          >
            메인으로
          </button>
        </header>
        <main className="flex-1 overflow-auto p-4 md:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
