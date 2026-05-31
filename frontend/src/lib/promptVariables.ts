import type { PromptTaskType } from "@/types/agentPrompt";

export interface PromptVariableDef {
  /** 템플릿에 삽입할 토큰 (예: {{company_name}}) */
  token: string;
  label: string;
  description: string;
  /** 미리보기용 예시 (실제 치환 값과 유사한 형태) */
  examplePreview: string;
}

const COMMON: PromptVariableDef[] = [
  {
    token: "{{company_name}}",
    label: "기업명",
    description:
      "상세 페이지의 대상 기업 이름입니다. DART 정식명과 다를 수 있으며, 인사말·제목 등에 넣기 좋습니다.",
    examplePreview: "삼성전자",
  },
  {
    token: "{{context}}",
    label: "수집 데이터 컨텍스트",
    description:
      "DART 동기화 후 생성되는 마크다운 블록입니다. 기업 개요, 재무제표(주요계정), 최근 공시, 관련 뉴스가 포함됩니다. 분석 근거로 반드시 포함하는 것을 권장합니다.",
    examplePreview:
      "## 기업\\n- 이름: 삼성전자\\n…\\n## 재무제표\\n…\\n## 최근 공시\\n…\\n## 관련 뉴스\\n…",
  },
];

const MARKET_TECHNICAL: PromptVariableDef = {
  token: "{{market_technical}}",
  label: "시장·기술 지표",
  description:
    "Yahoo Finance 일봉 기준 시세·등락률·RSI(14)·이동평균(5/20/60/120/240)·정배열/역배열·단기 추세 요약입니다. 상장 종목(ticker)이 있을 때만 채워집니다. 없으면 빈 문자열입니다.",
  examplePreview:
    "## 시장·기술 지표\\n- 금일가: 299,500원 (+2.15%)\\n- RSI(14): 58.3\\n- 60·120·240 추세: 정배열…",
};

const SIGNAL_JSON: PromptVariableDef = {
  token: "{{signal_json_instruction}}",
  label: "신호 JSON 출력 지침",
  description:
    "리스크·기회 신호를 JSON 형식으로만 출력하라는 고정 지침입니다. 템플릿 끝에 두면 출력 형식을 직접 제어할 수 있습니다. 없으면 서버가 동일 지침을 자동으로 덧붙입니다.",
  examplePreview:
    '{"risks":[{"label":"…","sources":["공시"]}],"opportunities":[…]}',
};

export const PROMPT_VARIABLES_BY_TASK: Record<PromptTaskType, PromptVariableDef[]> = {
  INSIGHT_SUMMARY: [...COMMON, MARKET_TECHNICAL],
  RISK_DETECTION: [...COMMON, MARKET_TECHNICAL, SIGNAL_JSON],
  RELATION_ANALYSIS: [...COMMON, MARKET_TECHNICAL],
};

export function extractUsedTokens(template: string): string[] {
  const matches = template.match(/\{\{[a-z_]+\}\}/g);
  if (!matches) return [];
  return [...new Set(matches)];
}

export function insertTokenAtCursor(
  textarea: HTMLTextAreaElement,
  currentValue: string,
  token: string,
  onChange: (value: string) => void
): void {
  const start = textarea.selectionStart ?? currentValue.length;
  const end = textarea.selectionEnd ?? start;
  const before = currentValue.slice(0, start);
  const after = currentValue.slice(end);
  const needsSpaceBefore = before.length > 0 && !/\s$/.test(before);
  const needsSpaceAfter = after.length > 0 && !/^\s/.test(after);
  const insertion =
    (needsSpaceBefore ? " " : "") + token + (needsSpaceAfter ? " " : "");
  const next = before + insertion + after;
  onChange(next);
  const cursor = before.length + insertion.length;
  requestAnimationFrame(() => {
    textarea.focus();
    textarea.setSelectionRange(cursor, cursor);
  });
}
