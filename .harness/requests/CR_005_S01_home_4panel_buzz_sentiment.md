# CR_005: S01 홈 4패널 (다 언급·공탐 지수·가로 확장)

## 원하는 것

- 홈 인사이트를 4패널 가로 배치: 인기 조회 / 다 언급 / 시장 뉴스 / 공탐 지수
- 좌우 `max-w-[1200px]` 제한 제거, 콘텐츠 폭 확장
- 다 언급: 네이버 금융 **인기검색** (A안)
- 공탐 지수: 코스피(Yahoo 추정) + 미국([CNN F&G](https://edition.cnn.com/markets/fear-and-greed) 공식 API)

## 구현 요약

### API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/home/buzz-companies` | 네이버 인기검색 + `companies` 매칭 |
| GET | `/api/v1/home/market-sentiment` | KOSPI 공탐 지수 0~100 + 7지표 |

### 다 언급

- `NaverPopularSearchClient` — `finance.naver.com/sise/` `popularItemList` 파싱 (EUC-KR)
- DB에 없는 티커는 이름만 표시 (`companyId` null)

### 공탐 지수

- **코스피**: `FearGreedIndexService` — 7지표 동일 가중 (`YAHOO_PROXY`), VIX는 `^VIX` 실시간
- **미국**: `CnnFearGreedClient` — `production.dataviz.cnn.io/index/fearandgreed/graphdata` (`CNN_OFFICIAL`), VIX·VIX MA50 포함
- 10분 메모리 캐시

### UI

- `HomeInsightsSection` — `min-[1400px]:grid-cols-4`
- `BuzzCompaniesPanel`, `SentimentIndexPanel` 신규
- 패널 높이 400px 통일 (`homePanelStyles.ts`)

## 상태

✅ 구현 완료 (2026-05-26)

## 후속

- KRX 일별 전종목 승인 후 breadth/strength/VKOSPI를 공식 데이터로 교체
- 공탐 지수 30일 타임라인 차트
