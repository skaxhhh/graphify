export type PromptTaskType =
  | "RELATION_ANALYSIS"
  | "RISK_DETECTION"
  | "INSIGHT_SUMMARY";

export interface AgentPromptVersion {
  id: number;
  versionNumber: number;
  createdAt: string;
  author: string;
  summary: string;
  changeNote: string | null;
}

export interface AgentPromptDetail {
  id: number;
  type: PromptTaskType;
  systemPrompt: string;
  taskTemplate: string;
  versions: AgentPromptVersion[];
}

export interface AgentPromptSavePayload {
  type: PromptTaskType;
  systemPrompt: string;
  taskTemplate: string;
  changeNote?: string;
}

export interface AgentPromptRollbackPayload {
  targetVersionId: number;
}

export interface AgentPromptTestPayload {
  companyId: number;
  sampleInput?: string;
}

export interface TokenUsage {
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
}

export interface AgentPromptTestResult {
  output: string;
  tokenUsage: TokenUsage;
  companyName: string;
}

export const PROMPT_TASK_TABS: { type: PromptTaskType; label: string }[] = [
  { type: "RELATION_ANALYSIS", label: "관계 분석" },
  { type: "RISK_DETECTION", label: "리스크 탐지" },
  { type: "INSIGHT_SUMMARY", label: "인사이트 요약" },
];

export const DEFAULT_PROMPT_DRAFTS: Record<
  PromptTaskType,
  { systemPrompt: string; taskTemplate: string }
> = {
  RELATION_ANALYSIS: {
    systemPrompt:
      "당신은 기업 관계 분석 전문 AI입니다. 모든 분석 결과는 한국어로 제공하며, 투자 판단의 근거로 활용될 수 있음을 인지하고 객관적 사실 기반으로 분석합니다.",
    taskTemplate:
      "대상 기업의 공급망·투자·협력 관계를 그래프 관점에서 요약하고, 핵심 연결 기업과 관계 유형을 설명하세요.",
  },
  RISK_DETECTION: {
    systemPrompt:
      "당신은 기업 리스크 탐지 전문 AI입니다. 공시·뉴스·재무·시장 기술 지표를 바탕으로 객관적으로 리스크 요인을 식별합니다.",
    taskTemplate: `대상 기업: {{company_name}}

{{context}}

{{market_technical}}

위 데이터를 근거로 리스크·기회 신호를 분석하세요.`,
  },
  INSIGHT_SUMMARY: {
    systemPrompt:
      "당신은 투자 인사이트 요약 전문 AI입니다. 분석 결과를 비전문가도 이해할 수 있게 간결히 정리합니다.",
    taskTemplate: `대상 기업: {{company_name}}

아래 DART·뉴스·시장 기술 지표만 근거로 투자·관계 인사이트를 작성하세요.
3~5개 불릿으로 요약하고, 각 항목 끝에 신뢰도(높음/중간/낮음)를 괄호로 표시하세요.

{{context}}

{{market_technical}}`,
  },
};
