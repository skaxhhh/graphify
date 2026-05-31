# API 명세 — Task 2: S01 홈/랜딩

> **구현 화면**: S01  
> **Base path**: `/api/v1/home` (신규), `/api/v1/search` (기존)  
> **CR**: CR_001, CR_005

## 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/search/autocomplete?q=` | 검색 자동완성 | Guest |
| GET | `/terms/latest` | 약관·기업 수 메타 | Guest |
| GET | `/home/trending-companies` | 인기 조회 기업 순위 | Guest |
| GET | `/home/buzz-companies` | 네이버 인기검색 기업 | Guest |
| GET | `/home/market-news` | 시장·기업 뉴스 피드 | Guest |
| GET | `/home/market-sentiment` | KOSPI 공탐 지수 (Fear & Greed) | Guest |

## GET /home/trending-companies

Query: `limit` (optional, default 8, max 20)

```json
{
  "success": true,
  "data": [
    {
      "rank": 1,
      "companyId": 1,
      "name": "삼성전자",
      "ticker": "005930",
      "industry": "반도체",
      "viewCount": 4820
    }
  ]
}
```

조회수는 `company_view_stats` 기준이며, 기업 상세 조회 시 +1.

## GET /home/buzz-companies

Query: `limit` (optional, default 8, max 20)

네이버 금융 시세 메인 `인기검색종목` HTML 파싱. `companies.ticker` 매칭 시 `companyId` 포함.

```json
{
  "success": true,
  "data": [
    {
      "rank": 1,
      "companyId": 1,
      "name": "삼성전자",
      "ticker": "005930",
      "industry": "반도체",
      "price": 299000,
      "priceDirection": "up",
      "sourceLabel": "네이버 금융 인기검색"
    }
  ]
}
```

## GET /home/market-news

Query: `limit` (optional, default 12, max 20)

**데이터 소스 (서버 동기화, 15분 캐시)**

1. `NEWS_API_KEY` 설정 시 NewsAPI 한국 비즈니스 헤드라인
2. 항상 한국 경제 RSS
3. `market_news` 테이블 upsert 후 응답

## GET /home/market-sentiment

**10분 캐시.** 듀얼 스냅샷:

| 필드 | 출처 | 설명 |
|------|------|------|
| `kospi` | `YAHOO_PROXY` | 국내 7지표 자체 산출 (KOSPI·KOSDAQ·VIX 등) |
| `nasdaq` | `CNN_OFFICIAL` | [CNN Fear & Greed](https://edition.cnn.com/markets/fear-and-greed) 공식 API (`production.dataviz.cnn.io`) — S&P500 기준 미국 시장 |

```json
{
  "success": true,
  "data": {
    "kospi": {
      "score": 70.5,
      "zone": "GREED",
      "zoneLabel": "탐욕",
      "market": "KOSPI",
      "indicators": [],
      "quoteTime": "2026-05-26T06:30:00Z",
      "dataSource": "YAHOO_PROXY",
      "vix": 16.8,
      "vixMa50": null
    },
    "nasdaq": {
      "score": 58.6,
      "zone": "GREED",
      "zoneLabel": "탐욕",
      "market": "US (CNN)",
      "indicators": [],
      "quoteTime": "2026-05-26T15:30:00Z",
      "dataSource": "CNN_OFFICIAL",
      "vix": 16.78,
      "vixMa50": 20.6
    },
    "asOf": "2026-05-26T07:00:00Z"
  }
}
```

> `nasdaq` 필드명은 API 호환용 유지. 실제 데이터는 CNN 미국 공탐 지수(S&P500)이며 순수 나스닥 지수가 아님.

**zone**: `EXTREME_FEAR` | `FEAR` | `NEUTRAL` | `GREED` | `EXTREME_GREED`

**오류**: `ERR_HOME_001` (502) — 지표 계산 실패

## 공통

- Bearer JWT 불필요 (Guest 허용)
- 응답: `{ success, data }` / 실패 `{ success: false, error }`
