import type { FinancialStatementLine } from "@/types/company";

interface CompanyFinancialQuarterlyTableProps {
  rows: FinancialStatementLine[];
}

const KEY_ACCOUNTS = ["매출액", "영업이익", "영업이익(손실)", "당기순이익", "당기순이익(손실)"];

function periodKey(row: FinancialStatementLine) {
  return `${row.bsnsYear}-${row.reprtCode ?? row.reportLabel}`;
}

function periodLabel(row: FinancialStatementLine) {
  const shortReport = row.reportLabel
    .replace("보고서", "")
    .replace("사업", "연간")
    .trim();
  return `${row.bsnsYear} ${shortReport}`;
}

function reportOrder(reprtCode: string | null | undefined): number {
  switch (reprtCode) {
    case "11014":
      return 4;
    case "11012":
      return 3;
    case "11013":
      return 2;
    case "11011":
      return 1;
    default:
      return 0;
  }
}

export function CompanyFinancialQuarterlyTable({ rows }: CompanyFinancialQuarterlyTableProps) {
  const filtered = rows.filter((row) =>
    KEY_ACCOUNTS.some((name) => row.accountName === name || row.accountName.startsWith(name))
  );

  const periodMap = new Map<string, { label: string; order: number }>();
  for (const row of filtered) {
    const key = periodKey(row);
    if (!periodMap.has(key)) {
      periodMap.set(key, {
        label: periodLabel(row),
        order: Number(row.bsnsYear) * 10 + reportOrder(row.reprtCode),
      });
    }
  }

  const periods = [...periodMap.entries()]
    .sort((a, b) => b[1].order - a[1].order)
    .map(([key, meta]) => ({ key, label: meta.label }));

  const accounts = [...new Set(filtered.map((row) => row.accountName))];

  const valueByPeriodAccount = new Map<string, string | null>();
  for (const row of filtered) {
    valueByPeriodAccount.set(`${periodKey(row)}|${row.accountName}`, row.currentAmount);
  }

  if (periods.length === 0 || accounts.length === 0) {
    return <p className="text-sm text-muted-gray">분기별 재무 데이터가 없습니다.</p>;
  }

  return (
    <div className="-mx-1 overflow-x-auto px-1">
      <table className="w-full min-w-full border-collapse text-sm">
        <thead>
          <tr className="border-b border-warm-border text-muted-gray">
            <th className="whitespace-nowrap py-2.5 pr-4 text-left font-medium">계정</th>
            {periods.map((period) => (
              <th
                key={period.key}
                className="whitespace-nowrap px-3 py-2.5 text-right font-medium"
              >
                {period.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {accounts.map((account) => (
            <tr key={account} className="border-b border-warm-border/50 text-charcoal">
              <td className="whitespace-nowrap py-2.5 pr-4 font-medium">{account}</td>
              {periods.map((period) => (
                <td
                  key={`${period.key}-${account}`}
                  className="whitespace-nowrap px-3 py-2.5 text-right tabular-nums"
                >
                  {valueByPeriodAccount.get(`${period.key}|${account}`) ?? "—"}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
