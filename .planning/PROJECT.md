# Graphify — 모의투자 고도화 & 토스증권 실투자 연동

## What This Is

Graphify는 국내 주식 시장 데이터를 기반으로 기업 그래프 분석, AI 에이전트 스트리밍, 자동매매 룰 기반 모의투자를 제공하는 금융 플랫폼이다. 현재 마일스톤은 부분 구현된 모의투자(백테스트 엔진·룰 CRUD)를 완전한 서비스로 고도화하고, 검증된 룰을 토스증권 Open API를 통해 실투자로 승격시키는 엔드-투-엔드 파이프라인을 완성하는 것이다.

## Core Value

룰 기반 자동매매 전략을 백테스트로 검증하고 → 실시간 모의 실행으로 성과를 확인한 뒤 → 토스증권 실계좌로 원클릭 승격할 수 있는 일관된 파이프라인이 동작해야 한다.

## Requirements

### Validated

- ✓ 백테스트 엔진 (BacktestService + RuleEvaluator + FillSimulator + PaperLedger) — 기존
- ✓ 룰 CRUD (TradingRule entity, PaperRuleController, PaperRuleService) — 기존
- ✓ 백테스트 기본 UI (룰 선택, 기간 설정, 거래 내역 테이블) — 기존
- ✓ DB 스키마 (V26-V29: trading_mode, trading_rules, paper_accounts, market_bars) — 기존
- ✓ MarketDataPort 인터페이스 (백테스트용 historicalDailyBars 추상화) — 기존

### Active

- [ ] 백테스트 수익률 곡선 차트 (Equity Curve 시각화)
- [ ] 실시간 시장 데이터 수집 (MarketDataPort 실시간 구현체, 분봉/일봉 갱신)
- [ ] 실시간 룰 평가 엔진 (스케줄러 기반, PAPER_LIVE 룰 자동 평가·가상 체결)
- [ ] 모의 대시보드 (가상 잔고, 활성 포지션, 미실현 손익 실시간 표시)
- [ ] 성과 리포트 페이지 (MDD, 승률, 스탑 비번, 기간별 수익률)
- [ ] 룰 생애주기 관리 (DRAFT → BACKTESTED → PAPER_LIVE → LIVE 상태 전환 UI/API)
- [ ] 토스증권 Open API 연동 (OAuth 인증, 종목 조회, 주문 실행, 잔고 조회)
- [ ] 실계좌 주문 실행 (LIVE 룰 평가 → 토스증권 API 주문 발행)

### Out of Scope

- 수동 주문 UI — 자동매매 룰 기반으로 결정, 수동 주문은 별도 마일스톤
- 모바일 앱 — 웹 우선
- 타 증권사 API (KIS 등) — 토스증권 먼저, 추후 확장
- 암호화폐 — 국내 주식 한정

## Context

**기존 구현 상태:**
- `BacktestService`: 일봉 기반 백테스트 완전 구현. 수익률·MDD·승률 계산됨.
- `RuleEvaluator`: SMA, EMA, RSI, crossAbove/crossBelow 지원. 볼륨 미지원.
- `PaperLedger`: 가상 계좌 원장. 매수/매도·손익 기록.
- `MarketDataPort`: 인터페이스만 있고 실시간 구현체 없음 (Yahoo Finance 히스토리컬만).
- 프론트엔드 paper 페이지들: PaperDashboardPage는 "준비 중" 빈 껍데기.
- PaperBacktestPage: 동작하지만 차트 없음 (숫자 테이블만).

**기술 스택:** Spring Boot 3.4.5 / Java 21 / PostgreSQL / React 18 / TypeScript / Zustand / Tailwind CSS

**시장 시간:** 국내 주식 09:00~15:30 KST. 장 중에만 실시간 룰 평가 유의미.

## Constraints

- **Tech**: Spring Boot 3.4.5, React 18 — 메이저 버전 업그레이드 없음
- **Data**: Yahoo Finance API로 히스토리컬 일봉 제공 중. 실시간 분봉은 별도 소스 필요.
- **Auth**: 토스증권 Open API OAuth 토큰을 DB에 암호화 저장해야 함 (평문 저장 금지)
- **Market Hours**: 실시간 엔진은 장 중(09:00~15:30 KST)에만 평가 실행

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 자동매매만 구현 (수동 주문 제외) | 룰 기반 파이프라인에 집중, 수동 주문은 별도 범위 | — Pending |
| 토스증권 Open API 선택 | 사용자 요구사항 | — Pending |
| 차트 라이브러리 선택 필요 | Recharts(현 스택 친화) vs TradingView Lightweight Charts(전문 차트) | — Pending |
| 실시간 엔진 스케줄러 방식 | Spring @Scheduled(분 단위) → 장 중 분봉 평가 | — Pending |

---
*Last updated: 2026-06-20 after initial project initialization*
