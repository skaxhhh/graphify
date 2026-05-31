import { CompanyFinancialQuarterlyTable } from "@/components/company/CompanyFinancialQuarterlyTable";
import type { CompanyDartProfile } from "@/types/company";

interface SectionProps {
  profile: CompanyDartProfile;
}

export function CompanyFinancialSection({ profile }: SectionProps) {
  if (profile.financialStatements.length === 0) {
    return null;
  }

  return (
    <section
      className="rounded-xl border border-warm-border bg-cream p-5 md:p-6"
      data-testid="financial-section"
    >
      <h2 className="text-sm font-semibold text-charcoal">재무제표 — 분기별 추이 (DART)</h2>
      <p className="mt-1 text-xs text-muted-gray">
        매출·영업이익·순이익 기준, 최근 2개 사업연도 분기 보고서
      </p>
      <div className="mt-4 w-full">
        <CompanyFinancialQuarterlyTable rows={profile.financialStatements} />
      </div>
    </section>
  );
}

export function CompanyDisclosureSection({ profile }: SectionProps) {
  if (profile.recentDisclosures.length === 0) {
    return null;
  }

  return (
    <section
      className="rounded-xl border border-warm-border bg-cream p-5 md:p-6"
      data-testid="disclosure-section"
    >
      <h2 className="text-sm font-semibold text-charcoal">관련 공시 (DART, 최근 6개월)</h2>
      <ul className="mt-4 max-h-[28rem] space-y-3 overflow-y-auto pr-1">
        {profile.recentDisclosures.map((item, index) => (
          <li
            key={item.receiptNo ?? `${item.reportName}-${index}`}
            className="border-b border-warm-border/60 pb-3 last:border-0 last:pb-0"
          >
            <p className="text-xs text-muted-gray">{item.receiptDate ?? "—"}</p>
            <p className="mt-1 text-sm font-medium leading-snug text-charcoal">{item.reportName}</p>
            {item.submitter ? (
              <p className="mt-0.5 text-xs text-muted-gray">{item.submitter}</p>
            ) : null}
          </li>
        ))}
      </ul>
    </section>
  );
}

export function CompanyNewsSection({ profile }: SectionProps) {
  if (profile.relatedNews.length === 0) {
    return null;
  }

  return (
    <section
      className="rounded-xl border border-warm-border bg-cream p-5 md:p-6"
      data-testid="news-section"
    >
      <h2 className="text-sm font-semibold text-charcoal">관련 뉴스</h2>
      <ul className="mt-4 max-h-[32rem] space-y-3 overflow-y-auto pr-1">
        {profile.relatedNews.map((item) => (
          <li key={item.sourceUrl} className="border-b border-warm-border/60 pb-3 last:border-0 last:pb-0">
            <a
              href={item.sourceUrl}
              target="_blank"
              rel="noreferrer"
              className="text-sm font-medium leading-snug text-charcoal underline-offset-2 hover:underline"
            >
              {item.title}
            </a>
            <p className="mt-1 text-xs text-muted-gray">
              {item.sourceName}
              {item.publishedAt
                ? ` · ${new Date(item.publishedAt).toLocaleString("ko-KR")}`
                : ""}
            </p>
            {item.summary ? (
              <p className="mt-1 line-clamp-2 text-sm text-muted-gray">{item.summary}</p>
            ) : null}
          </li>
        ))}
      </ul>
    </section>
  );
}

export function CompanyDataCollectingSection() {
  return (
    <section
      className="rounded-xl border border-warm-border bg-cream p-6"
      aria-busy="true"
      data-testid="company-related-data-loading"
    >
      <p className="text-sm text-muted-gray">공시·재무·뉴스 수집 중…</p>
    </section>
  );
}
