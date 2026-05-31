import { Link } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";

export function AppHeaderUser() {
  const logout = useAuthStore((s) => s.logout);

  return (
    <header className="sticky top-0 z-50 flex h-16 items-center gap-4 border-b border-warm-border bg-cream px-4 md:px-8">
      <Link to="/" className="shrink-0 text-lg font-semibold text-charcoal">
        graphify
      </Link>
      <div className="mx-auto hidden max-w-xl flex-1 md:block">
        <input
          type="search"
          placeholder="기업 검색 (T02 S01)"
          className="w-full rounded-md border border-warm-border bg-cream-surface px-3 py-2 text-sm text-charcoal placeholder:text-muted-gray focus:outline-none focus:ring-2 focus:ring-ring-blue"
          readOnly
        />
      </div>
      <nav className="flex items-center gap-3 text-sm">
        <Link to="/history" className="text-charcoal hover:underline">
          분석 이력
        </Link>
        <Link to="/watchlist" className="text-charcoal hover:underline">
          관심 기업
        </Link>
        <Link to="/mypage" className="text-charcoal hover:underline">
          마이페이지
        </Link>
        <button
          type="button"
          onClick={logout}
          className="rounded-md border border-charcoal/40 px-3 py-1.5 text-charcoal hover:opacity-80"
        >
          로그아웃
        </button>
      </nav>
    </header>
  );
}
