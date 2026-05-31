# 개발 가이드 — T02 / S01 홈

> **화면**: S01 `/`  
> **태스크 ID**: T02_S01  
> **CR**: CR_001 (2패널), CR_005 (4패널·다언급·공탐)

## 레이아웃

- 히어로·검색: 중앙 `max-w-[720px]` 유지
- 인사이트 4패널: **풀 너비** (`HomeInsightsSection` — `max-w-[1200px]` 없음)
- `min-[1400px]:grid-cols-4` — 와이드 화면 4열, 그 미만 2열/1열

## 패널

| 패널 | 컴포넌트 | API |
|------|----------|-----|
| 인기 조회 | `TrendingCompaniesPanel` | `/home/trending-companies` |
| 다 언급 | `BuzzCompaniesPanel` | `/home/buzz-companies` |
| 시장 뉴스 | `MarketNewsPanel` | `/home/market-news` |
| 공탐 지수 | `SentimentIndexPanel` | `/home/market-sentiment` |

공통 스타일: `homePanelStyles.ts` → `HOME_PANEL_CARD_CLASS` (400px)

## 백엔드

| 클래스 | 역할 |
|--------|------|
| `NaverPopularSearchClient` | 네이버 인기검색 파싱 |
| `FearGreedIndexService` | 7지표 공탐 점수 |
| `YahooIndexSeriesClient` | 지수 일봉 (^KS11 등) |
| `HomeService` | 집계·캐시 |

## 로컬 확인

```bash
./init.sh restart
curl -s http://localhost:8081/api/v1/home/trending-companies | jq '.data | length'
curl -s http://localhost:8081/api/v1/home/buzz-companies | jq '.data[0]'
curl -s http://localhost:8081/api/v1/home/market-sentiment | jq '.data.score, .data.zoneLabel, (.data.indicators | length)'
```

프론트: `npm run dev` → `/`

## 후속 CR

- KRX Open API 승인 후 공탐 지표를 전종목·VKOSPI 공식 데이터로 교체
- 다 언급: 종목토론 활동량(B안) 추가
- 공탐 30일 타임라인 차트
