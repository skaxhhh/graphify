import type { CompanyDetail } from "@/types/company";

interface CompanyInfoSectionProps {
  company: CompanyDetail;
}

export function CompanyInfoSection({ company }: CompanyInfoSectionProps) {
  const financials = company.financials;

  return (
    <section className="rounded-xl border border-warm-border bg-cream p-5 md:p-6">
      <h2 className="text-sm font-semibold text-charcoal">기본 정보</h2>
      <p className="mt-3 text-sm leading-relaxed text-muted-gray">
        {company.summary || "사업 요약 정보가 없습니다."}
      </p>
      {company.dartProfile ? (
        <dl className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
          {company.dartProfile.ceoName ? (
            <>
              <dt className="text-muted-gray">대표이사</dt>
              <dd className="text-charcoal">{company.dartProfile.ceoName}</dd>
            </>
          ) : null}
          {company.dartProfile.corpClassLabel ? (
            <>
              <dt className="text-muted-gray">시장</dt>
              <dd className="text-charcoal">{company.dartProfile.corpClassLabel}</dd>
            </>
          ) : null}
          {company.dartProfile.industryCode ? (
            <>
              <dt className="text-muted-gray">업종코드</dt>
              <dd className="text-charcoal">{company.dartProfile.industryCode}</dd>
            </>
          ) : null}
          {company.dartProfile.estDate ? (
            <>
              <dt className="text-muted-gray">설립일</dt>
              <dd className="text-charcoal">{company.dartProfile.estDate}</dd>
            </>
          ) : null}
          {company.dartProfile.homepage ? (
            <>
              <dt className="text-muted-gray">홈페이지</dt>
              <dd className="truncate text-charcoal">
                <a
                  href={
                    company.dartProfile.homepage.startsWith("http")
                      ? company.dartProfile.homepage
                      : `https://${company.dartProfile.homepage}`
                  }
                  target="_blank"
                  rel="noreferrer"
                  className="underline hover:opacity-80"
                >
                  {company.dartProfile.homepage}
                </a>
              </dd>
            </>
          ) : null}
          {company.dartProfile.address ? (
            <>
              <dt className="text-muted-gray sm:col-span-1">주소</dt>
              <dd className="text-charcoal sm:col-span-1">{company.dartProfile.address}</dd>
            </>
          ) : null}
        </dl>
      ) : null}
      {financials ? (
        <div className="mt-6 overflow-x-auto">
          <table className="w-full min-w-[320px] text-left text-sm">
            <thead>
              <tr className="border-b border-warm-border text-muted-gray">
                <th className="py-2 pr-4 font-medium">구분</th>
                <th className="py-2 pr-4 font-medium">매출</th>
                <th className="py-2 pr-4 font-medium">영업이익률</th>
                <th className="py-2 font-medium">순이익률</th>
              </tr>
            </thead>
            <tbody>
              <tr className="text-charcoal">
                <td className="py-3 pr-4">{financials.periodLabel}</td>
                <td className="py-3 pr-4">{financials.revenue}</td>
                <td className="py-3 pr-4">{financials.operatingMargin}</td>
                <td className="py-3">{financials.netIncome}</td>
              </tr>
            </tbody>
          </table>
        </div>
      ) : null}
      <p className="mt-4 text-xs text-muted-gray">
        최신 업데이트: {new Date(company.lastUpdated).toLocaleString("ko-KR")}
      </p>
    </section>
  );
}
