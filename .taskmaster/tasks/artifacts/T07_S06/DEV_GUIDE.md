# 개발 가이드 — Task 7: S06 검색 결과 목록 — 정렬·필터·하이브리드 검색

## UI 명세

- [UI_SPEC — S06](../../../docs/ui_specs/UI_SPEC_S06_SearchResult.md)

## 구현 순서

1. UI_SPEC **레이아웃 구조** 및 breakpoint
2. **loading / empty / error / populated** 상태
3. **인터랙션 규칙** (해당 UI_SPEC 표)
4. `API_SPEC.md`와 Spring Controller DTO 정합

## 스택

- React + Spring Boot + PostgreSQL

### 완료 기준 (필수)
- [x] UI_SPEC 레이아웃 구조 준수
- [x] 4가지 상태(loading / empty / error / populated) 구현
- [x] 인터랙션 규칙 구현 (해당 화면 UI_SPEC 기준)
- [x] 이전·다음 태스크 인터페이스 연동 확인 (라우팅·API 계약·세션 상태)

## 구현 메모

- API: `GET /api/v1/companies/search` (`enrich=true` 기본), `POST /companies/resolve`, `POST /companies/{id}/sync`
- Autocomplete: `GET /api/v1/search/autocomplete` — **DB only**
- 외부: Open DART (`DART_API_KEY` → 루트 `.env`, `init.sh`가 source)
- DB: `V21__company_external_registry.sql`
- S01 `GlobalSearchBar` → autocomplete DB / submit → S06 enrich search
- 결과 행 클릭 → S07; stub 기업은 `needsSync` 후 sync API 가능
- 필터 변경 debounce 300ms 후 URL·재조회
- 시맨틱 힌트: 결과 3건 미만 시 `relatedQueries`, `similarCompanies` 응답

