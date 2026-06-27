import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useTradingStore } from "@/stores/tradingStore";
import { fetchTradingSettings } from "@/lib/tradingApi";
import { PaperTradingToggle } from "@/components/trading/PaperTradingToggle";
import { TradeModeIndicator } from "@/components/trading/ui/TradeModeIndicator";

interface NavItem {
  to: string;
  label: string;
  end?: boolean;
}

// D4: 메뉴 명칭 통일 — "전략 설정 / 전략 운영" 채택
// D3: 동작 모니터링 → PAPER 그룹으로 이동
const paperItems: NavItem[] = [
  { to: "/trading/paper/dashboard", label: "모의 대시보드" },
  { to: "/trading/paper/rules", label: "전략 설정" },
  { to: "/trading/paper/rules-lifecycle", label: "전략 운영" },
  { to: "/trading/paper/backtest", label: "백테스트" },
  { to: "/trading/monitor", label: "동작 모니터링" }, // D3: PAPER 그룹
  { to: "/trading/paper/history", label: "모의 거래 이력" },
  { to: "/trading/paper/report", label: "모의 성과 리포트" },
];

// D4: "현재 룰" → "전략 운영"; REMOVE monitor + 룰 수정
const liveItems: NavItem[] = [
  { to: "/trading/dashboard", label: "대시보드" },
  { to: "/trading/history", label: "거래 이력" },
  { to: "/trading/rules", label: "전략 운영" },
];

const commonItems: NavItem[] = [
  { to: "/trading", label: "DDS Agent", end: true },
  { to: "/trading/settings", label: "토스 설정" },
];

function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const mode = useTradingStore((s) => s.mode);
  const modeItems = mode === "PAPER" ? paperItems : liveItems;
  const modeGroupLabel = mode === "PAPER" ? "PAPER" : "LIVE";

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-2.5 rounded-md px-3 py-2 text-sm transition-colors font-trade-sans ${
      isActive
        ? "bg-trade-elevated border-l-2 border-trade-primary text-trade-primary font-semibold"
        : "text-trade-muted hover:bg-trade-elevated hover:text-trade-body"
    }`;

  return (
    <nav className="flex flex-1 flex-col gap-1">
      <p className="px-3 pb-2 text-xs font-semibold text-trade-muted tracking-wider font-trade-sans">
        {modeGroupLabel}
      </p>
      {modeItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end ?? false}
          onClick={onNavigate}
          className={navLinkClass}
        >
          {item.label}
        </NavLink>
      ))}

      <p className="px-3 pb-2 pt-4 text-xs font-semibold text-trade-muted tracking-wider font-trade-sans">
        공통
      </p>
      {commonItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end ?? false}
          onClick={onNavigate}
          className={navLinkClass}
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}

function SidebarHeader() {
  return (
    <div className="border-b border-trade-hairline p-4 flex items-center gap-2">
      <p className="text-lg font-bold text-trade-primary font-trade-sans">
        graphify
      </p>
      <span className="rounded border border-trade-primary/40 px-1.5 py-0.5 text-xs font-semibold text-trade-primary font-trade-sans">
        BETA
      </span>
    </div>
  );
}

function SidebarFooter({ onExit }: { onExit: () => void }) {
  return (
    <div className="border-t border-trade-hairline">
      <PaperTradingToggle />
      <div className="p-3">
        <button
          type="button"
          onClick={onExit}
          className="flex items-center gap-2 text-sm text-trade-muted hover:text-trade-body font-trade-sans transition-colors"
        >
          <span>←</span>
          <span>메인으로</span>
        </button>
      </div>
    </div>
  );
}

export function TradingLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const navigate = useNavigate();
  const disableDarkMode = useTradingStore((s) => s.disableDarkMode);
  const setMode = useTradingStore((s) => s.setMode);

  // 진입 시 서버의 트레이딩 모드를 동기화 (보존됨)
  useEffect(() => {
    let active = true;
    void fetchTradingSettings()
      .then((res) => {
        if (active && res.data?.tradingMode) {
          setMode(res.data.tradingMode);
        }
      })
      .catch(() => {
        /* 권한 없거나 실패 시 기본값(PAPER) 유지 */
      });
    return () => {
      active = false;
    };
  }, [setMode]);

  const handleExit = () => {
    disableDarkMode();
    navigate("/");
  };

  const sidebarContent = (
    <>
      <SidebarHeader />
      <div className="flex flex-1 flex-col gap-1 overflow-y-auto p-3">
        {/* D8: mode indicator at the top of sidebar below header */}
        <TradeModeIndicator />
        <SidebarNav />
      </div>
      <SidebarFooter onExit={handleExit} />
    </>
  );

  return (
    <div className="flex min-h-screen bg-trade-bg text-trade-body font-trade-sans">
      {/* 데스크탑 사이드바 */}
      <aside className="hidden w-64 shrink-0 border-r border-trade-hairline bg-trade-surface lg:flex lg:flex-col">
        {sidebarContent}
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
          <aside className="relative flex h-full w-64 flex-col border-r border-trade-hairline bg-trade-surface shadow-xl">
            <SidebarHeader />
            <div className="flex flex-1 flex-col gap-1 overflow-y-auto p-3">
              <TradeModeIndicator />
              <SidebarNav onNavigate={() => setDrawerOpen(false)} />
            </div>
            <SidebarFooter onExit={handleExit} />
          </aside>
        </div>
      ) : null}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-trade-hairline bg-trade-surface px-4 md:px-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="rounded-md border border-trade-hairline px-2 py-1 text-sm text-trade-muted lg:hidden"
              onClick={() => setDrawerOpen(true)}
              aria-label="메뉴 열기"
            >
              ☰
            </button>
            <h1 className="text-sm font-semibold text-trade-body font-trade-sans">
              자동매매 봇
            </h1>
            <span className="rounded border border-trade-primary/40 px-2 py-0.5 text-xs font-semibold text-trade-primary font-trade-sans">
              BETA
            </span>
          </div>
          <button
            type="button"
            onClick={handleExit}
            className="hidden text-sm text-trade-muted hover:text-trade-body font-trade-sans transition-colors sm:inline"
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
