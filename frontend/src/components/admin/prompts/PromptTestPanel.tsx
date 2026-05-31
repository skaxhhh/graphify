import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { fetchCompanySearch } from "@/lib/searchApi";
import { useDebounce } from "@/hooks/useDebounce";
import type { AgentPromptTestResult } from "@/types/agentPrompt";

interface PromptTestPanelProps {
  open: boolean;
  onClose: () => void;
  testing: boolean;
  testResult: AgentPromptTestResult | null;
  onRunTest: (companyId: number, sampleInput: string) => void;
}

export function PromptTestPanel({
  open,
  onClose,
  testing,
  testResult,
  onRunTest,
}: PromptTestPanelProps) {
  const [companyQuery, setCompanyQuery] = useState("");
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
  const [selectedCompanyName, setSelectedCompanyName] = useState("");
  const [sampleInput, setSampleInput] = useState("");

  const debouncedQuery = useDebounce(companyQuery, 250);

  const companiesQuery = useQuery({
    queryKey: ["admin", "prompt-test-companies", debouncedQuery],
    queryFn: async () => {
      if (debouncedQuery.trim().length < 2) return [];
      const res = await fetchCompanySearch({
        q: debouncedQuery,
        page: 0,
        size: 8,
      });
      return res.data?.items ?? [];
    },
    enabled: open && debouncedQuery.trim().length >= 2,
  });

  const companies = companiesQuery.data ?? [];

  return (
    <>
      {open ? (
        <button
          type="button"
          className="fixed inset-0 z-40 bg-charcoal/20 lg:hidden"
          aria-label="테스트 패널 닫기"
          onClick={onClose}
        />
      ) : null}
      <div
        className={`fixed inset-y-0 right-0 z-50 flex w-full max-w-[480px] flex-col border-l border-warm-border bg-cream shadow-[0_0_24px_rgba(0,0,0,0.06)] transition-transform duration-300 ease-out ${
          open ? "translate-x-0" : "translate-x-full pointer-events-none"
        }`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="prompt-test-title"
      >
        <div className="flex h-14 items-center justify-between border-b border-warm-border px-4">
          <h2 id="prompt-test-title" className="text-sm font-semibold text-charcoal">
            프롬프트 테스트 (OV04)
          </h2>
          <button
            type="button"
            className="text-sm text-muted-gray underline"
            onClick={onClose}
          >
            닫기
          </button>
        </div>

        <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4">
          <div className="space-y-2">
            <label className="text-sm font-medium text-charcoal">기업 검색</label>
            <input
              type="search"
              value={companyQuery}
              onChange={(e) => {
                setCompanyQuery(e.target.value);
                setSelectedCompanyId(null);
                setSelectedCompanyName("");
              }}
              placeholder="2자 이상 입력"
              className="w-full rounded-md border border-warm-border bg-cream-surface px-3 py-2 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue"
            />
            {selectedCompanyId != null ? (
              <p className="text-xs text-charcoal">
                선택: {selectedCompanyName} (id {selectedCompanyId})
              </p>
            ) : null}
            {companiesQuery.isLoading ? (
              <SkeletonBlock className="h-20 w-full rounded-md" />
            ) : companies.length > 0 ? (
              <ul className="max-h-40 space-y-1 overflow-y-auto rounded-md border border-warm-border bg-cream-surface p-2">
                {companies.map((c) => (
                  <li key={c.id}>
                    <button
                      type="button"
                      className="w-full rounded px-2 py-1.5 text-left text-sm text-charcoal hover:bg-charcoal/5"
                      onClick={() => {
                        setSelectedCompanyId(c.id);
                        setSelectedCompanyName(c.name);
                        setCompanyQuery(c.name);
                      }}
                    >
                      {c.name}
                      {c.ticker ? (
                        <span className="ml-2 text-xs text-muted-gray">{c.ticker}</span>
                      ) : null}
                    </button>
                  </li>
                ))}
              </ul>
            ) : debouncedQuery.trim().length >= 2 ? (
              <p className="text-xs text-muted-gray">검색 결과가 없습니다.</p>
            ) : null}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-charcoal">
              추가 입력 (선택)
            </label>
            <textarea
              value={sampleInput}
              onChange={(e) => setSampleInput(e.target.value)}
              rows={3}
              className="w-full resize-y rounded-md border border-warm-border bg-cream-surface px-3 py-2 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue"
              placeholder="테스트용 추가 지시"
            />
          </div>

          <PrimaryButton
            type="button"
            loading={testing}
            disabled={selectedCompanyId == null}
            onClick={() => {
              if (selectedCompanyId != null) {
                onRunTest(selectedCompanyId, sampleInput);
              }
            }}
          >
            테스트 실행
          </PrimaryButton>

          <div className="min-h-[120px] rounded-lg border border-warm-border bg-cream-surface p-3">
            <p className="mb-2 text-xs font-medium text-muted-gray">출력</p>
            {testing ? (
              <div className="space-y-2" aria-live="polite">
                <SkeletonBlock className="h-3 w-full rounded" />
                <SkeletonBlock className="h-3 w-5/6 rounded" />
                <SkeletonBlock className="h-3 w-4/6 rounded" />
                <span className="inline-block animate-pulse text-muted-gray">▍</span>
              </div>
            ) : testResult ? (
              <div className="space-y-2">
                <p className="whitespace-pre-wrap font-mono text-xs text-charcoal">
                  {testResult.output}
                </p>
                <p className="text-xs text-muted-gray">
                  토큰: in {testResult.tokenUsage.inputTokens} / out{" "}
                  {testResult.tokenUsage.outputTokens} (합계{" "}
                  {testResult.tokenUsage.totalTokens})
                </p>
              </div>
            ) : (
              <p className="text-sm text-muted-gray">
                기업을 선택한 뒤 테스트를 실행하세요.
              </p>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
