import { Link } from "react-router-dom";

export function MarketingFooter() {
  return (
    <footer className="border-t border-warm-border px-4 py-8 md:px-8">
      <div className="mx-auto flex max-w-[1200px] flex-col gap-4 text-sm text-muted-gray md:flex-row md:items-center md:justify-between">
        <p>© graphify — AI 생성 결과는 참고용입니다.</p>
        <nav className="flex flex-wrap gap-4">
          <Link to="/terms" className="underline hover:text-charcoal">
            이용약관
          </Link>
          <Link to="/privacy" className="underline hover:text-charcoal">
            개인정보처리방침
          </Link>
          <span>면책 안내</span>
        </nav>
      </div>
    </footer>
  );
}
