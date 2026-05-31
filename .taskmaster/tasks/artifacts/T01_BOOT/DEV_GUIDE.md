# 개발 가이드 — Task 1: graphify 모노레포 부트스트랩 (React + Spring Boot + PostgreSQL)

## UI 명세

- 공통 레이아웃: [SCREEN_FLOW.md](../../../docs/SCREEN_FLOW.md) (§3 공유 레이아웃)
- 전역 정책: [prd.txt](../../../docs/prd.txt) (§3 아키텍처)

## 구현 순서

1. 모노레포/멀티모듈 디렉터리 구조 확정
2. 로컬 PostgreSQL 및 Spring datasource
3. React 라우터·레이아웃 셸(Guest/User/Admin 분기 준비)
4. `API_SPEC.md` 헬스 확인

## 스택

- React + Spring Boot + PostgreSQL

## 로컬 실행

```bash
# 1. DB
docker compose up -d

# 2. API (JDK 21 — Gradle toolchain 자동 다운로드)
cd backend && ./gradlew bootRun --args='--server.port=8081'

# 3. Web
cd frontend && npm install && npm run dev
```

- 헬스: `GET http://localhost:8081/actuator/health` → `{"status":"UP"}`
- Bootstrap: `GET http://localhost:8081/api/v1/bootstrap/status`
- UI: `http://localhost:5173/bootstrap` (4상태·API 연동 확인)

## 디렉터리 구조

```
backend/          # Spring Boot 3.4, Java 21, Flyway
frontend/         # Vite + React 18 + TS + Tailwind
docker-compose.yml
.env.example
```

### 완료 기준 (필수)

- [x] UI_SPEC 레이아웃 구조 준수 — SCREEN_FLOW §3 Guest/User/Admin 레이아웃 셸 (`frontend/src/layouts/`)
- [x] 4가지 상태(loading / empty / error / populated) — `PageState` + `/bootstrap` 페이지
- [x] 인터랙션 규칙 — 부트스트랩 범위: 재시도 버튼, 라우트 전환 (후속 S##에서 화면별 상세)
- [x] 이전·다음 태스크 인터페이스 — 공통 API 응답 포맷, 라우트 placeholder (S01~S17), `authStore` 스텁
