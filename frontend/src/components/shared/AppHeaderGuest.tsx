import type { MouseEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";

interface AppHeaderGuestProps {
  hideLoginButton?: boolean;
}

export function AppHeaderGuest({ hideLoginButton = false }: AppHeaderGuestProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);
  const role = useAuthStore((s) => s.role);
  const logout = useAuthStore((s) => s.logout);

  const handleLogoClick = (event: MouseEvent<HTMLAnchorElement>) => {
    if (location.pathname === "/") {
      event.preventDefault();
      window.scrollTo({ top: 0, behavior: "auto" });
    } else {
      navigate("/");
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <header className="sticky top-0 z-50 flex h-16 items-center justify-between border-b border-warm-border bg-cream px-4 md:px-8">
      <Link
        to="/"
        onClick={handleLogoClick}
        className="text-lg font-semibold tracking-tight text-charcoal"
      >
        graphify
      </Link>
      {isAuthenticated && user ? (
        <nav className="flex items-center gap-2 text-sm sm:gap-3">
          {role === "admin" ? (
            <Link
              to="/admin"
              className="hidden rounded-md border border-charcoal/40 px-3 py-1.5 text-charcoal transition-opacity hover:opacity-80 sm:inline"
            >
              관리자 페이지
            </Link>
          ) : null}
          <Link to="/history" className="hidden text-charcoal hover:underline sm:inline">
            분석 이력
          </Link>
          <Link to="/watchlist" className="hidden text-charcoal hover:underline sm:inline">
            관심 기업
          </Link>
          <Link to="/mypage" className="text-charcoal hover:underline">
            마이페이지
          </Link>
          <span className="hidden max-w-[100px] truncate text-muted-gray md:inline">
            {user.displayName}
          </span>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-md border border-charcoal/40 px-3 py-1.5 text-charcoal transition-opacity hover:opacity-80"
          >
            로그아웃
          </button>
        </nav>
      ) : hideLoginButton ? (
        <span className="w-[72px]" aria-hidden />
      ) : (
        <Link
          to="/login"
          className="rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-btn-inset transition-opacity hover:opacity-90"
        >
          로그인
        </Link>
      )}
    </header>
  );
}
