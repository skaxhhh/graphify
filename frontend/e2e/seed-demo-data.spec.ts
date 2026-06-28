/**
 * seed-demo-data.spec.ts — UI 기반 데모 데이터 시더 (운영/스테이징 배포 후 실행용).
 *
 * 일반 e2e 테스트가 아니라, 실제 백엔드를 상대로 UI를 구동해
 * "전략 설정"(룰 CRUD + DRAFT→ACTIVE)과 "전략 운영"(시작 → RUNNING) 예시 데이터를 만든다.
 * → 배포 환경에서도 화면에 예시 룰/운영 상태가 보이도록 한다.
 *
 * ⚠️ 이 스펙은 API를 mock하지 않는다. 진짜 계정으로 로그인하고 진짜 룰을 생성한다.
 *    재실행 시 같은 이름의 룰이 중복 생성된다(멱등 아님).
 *
 * ── 실행 방법 ──────────────────────────────────────────────────────────────
 *   SEED_RUN=1 \
 *   PLAYWRIGHT_BASE_URL="https://운영-도메인" \
 *   SEED_EMAIL="you@example.com" \
 *   SEED_PASSWORD="********" \
 *   PLAYWRIGHT_SKIP_WEBSERVER=1 \
 *   npx playwright test e2e/seed-demo-data.spec.ts --project=chromium
 *
 *   - SEED_RUN 미설정 시 스킵된다(일반 `npm run test:e2e`에 섞이지 않음).
 *   - PLAYWRIGHT_SKIP_WEBSERVER=1: 로컬 dev 서버를 띄우지 말고 BASE_URL로 바로 접속.
 *   - 로컬에서 띄워둔 서버로 시드하려면 BASE_URL 기본값(http://localhost:5173) 사용.
 *
 * ── "전략 운영"(RUNNING) 주의 ──────────────────────────────────────────────
 *   start()에는 폐장 가드가 있다(평일 09:00–18:00 KST, 공휴일 제외).
 *   그 시간대 밖에서 실행하면 "시작"이 ERR_MARKET_CLOSED로 거부되어
 *   RUNNING 예시는 생성되지 않는다(룰 생성/활성화는 정상). 콘솔에 경고를 남긴다.
 *   RUNNING 예시까지 필요하면 평일 장중에 실행할 것.
 */

import { expect, test, type Page } from "@playwright/test";

const SEED_RUN = process.env.SEED_RUN === "1";
const EMAIL = process.env.SEED_EMAIL ?? "";
const PASSWORD = process.env.SEED_PASSWORD ?? "";

// 생성할 예시 룰. symbols 유니버스를 써서 시작 시 실시간 랭킹/종목선택 모달을 우회한다.
// (volume_top_n은 라이브 랭킹 실패 시 종목 직접선택 모달이 떠서 시더가 복잡해진다)
const DEMO_RULES: { name: string; symbols: string[]; fast: number; slow: number }[] = [
  { name: "[예시] 삼성전자 SMA 5/20 골든크로스", symbols: ["005930"], fast: 5, slow: 20 },
  { name: "[예시] 반도체 모멘텀 (삼성·하이닉스)", symbols: ["005930", "000660"], fast: 10, slow: 30 },
  { name: "[예시] 현대차·기아 추세추종", symbols: ["005380", "000270"], fast: 5, slow: 60 },
];

function ruleDefinition(symbols: string[], fast: number, slow: number) {
  return {
    version: 1,
    universe: { type: "symbols", symbols, market: "KOSPI" },
    entry: {
      logic: "AND",
      conditions: [
        {
          left: { indicator: "SMA", params: { period: fast } },
          op: ">",
          right: { indicator: "SMA", params: { period: slow } },
        },
      ],
    },
    sizing: { type: "cash", value: 1000000 },
  };
}

/**
 * 로그인: API(/api/v1/auth/login)로 토큰을 받아 앱과 동일한 localStorage 키에 주입한다.
 * UI 로그인 폼(이메일/OAuth 게이팅·라벨 매칭)에 의존하지 않아 운영/스테이징에서 안정적이다.
 * 앱의 persistSession과 동일 키: graphify.accessToken / refreshToken / user.
 */
async function login(page: Page) {
  const res = await page.request.post("/api/v1/auth/login", {
    data: { email: EMAIL, password: PASSWORD },
  });
  if (!res.ok()) {
    throw new Error(`로그인 실패: HTTP ${res.status()} ${await res.text()}`);
  }
  const body = (await res.json()) as {
    data?: { accessToken: string; refreshToken: string; user: unknown };
  };
  const d = body.data;
  if (!d?.accessToken) throw new Error("로그인 응답에 accessToken이 없습니다.");

  // 앱 origin을 한 번 로드한 뒤 토큰 주입(localStorage는 origin 스코프)
  await page.goto("/login");
  await page.evaluate(
    ({ a, r, u }) => {
      localStorage.setItem("graphify.accessToken", a);
      localStorage.setItem("graphify.refreshToken", r);
      localStorage.setItem("graphify.user", JSON.stringify(u));
    },
    { a: d.accessToken, r: d.refreshToken, u: d.user }
  );
}

/** PAPER 모드 보장(전략 메뉴 노출). 모드 토글이 LIVE면 모의로 전환. */
async function ensurePaperMode(page: Page) {
  await page.goto("/trading/paper/rules");
  // "전략 설정" 화면의 "+ 새 룰"이 보이면 PAPER 모드 OK
  const newRuleBtn = page.getByRole("button", { name: /새 룰/ });
  if (await newRuleBtn.isVisible().catch(() => false)) return;
  // 아니라면 모의 토글 시도(있을 때만)
  const paperToggle = page.getByRole("button", { name: /모의/ }).first();
  if (await paperToggle.isVisible().catch(() => false)) {
    await paperToggle.click();
    await page.goto("/trading/paper/rules");
  }
}

/** 전략 설정: 새 룰 생성(JSON 탭으로 정의 주입) → 저장. */
async function createRule(
  page: Page,
  rule: { name: string; symbols: string[]; fast: number; slow: number }
) {
  await page.goto("/trading/paper/rules/new");

  // ① 룰 이름 (빌더 탭)
  await page.getByPlaceholder("예: KOSPI Top10 모멘텀 전략").fill(rule.name);

  // 정의는 JSON 탭에서 주입(빌더 폼 조작보다 안정적) → 빌더로 되돌리며 검증
  await page.getByRole("button", { name: "JSON" }).click();
  const json = JSON.stringify(ruleDefinition(rule.symbols, rule.fast, rule.slow), null, 2);
  const textarea = page.locator("textarea").first();
  await textarea.fill(json);
  // 빌더 탭으로 복귀하면 JSON이 파싱되어 builderState에 반영된다(이름은 유지)
  await page.getByRole("button", { name: "빌더" }).click();
  // ⚠️ JSON→빌더 파싱은 비동기 state 갱신 — 반영 전에 저장하면 검증 실패한다.
  //    종목 입력칸에 값이 반영될 때까지 대기해 race를 방지.
  await expect(page.getByPlaceholder("005930,000660,035720")).toHaveValue(
    rule.symbols.join(","),
    { timeout: 10_000 }
  );

  // 저장 → 성공 시 /trading/paper/rules로 이동. 실패 시 formError 텍스트를 그대로 노출.
  await page.getByRole("button", { name: "저장", exact: true }).click();
  const errorP = page.locator("p.text-trade-down").first();
  const outcome = await Promise.race([
    page
      .waitForURL(/\/trading\/paper\/rules(\?|$)/, { timeout: 30_000 })
      .then(() => "ok" as const),
    errorP.waitFor({ state: "visible", timeout: 30_000 }).then(() => "err" as const),
  ]).catch(() => "timeout" as const);
  if (outcome !== "ok") {
    const msg = await errorP.textContent().catch(() => null);
    throw new Error(`룰 저장 실패 (${outcome}): ${msg ?? "(메시지 없음)"}`);
  }
  await expect(page.getByText(rule.name).first()).toBeVisible({ timeout: 15_000 });
}

/**
 * 행 로케이터: 룰 행은 test-id 없는 <div>(TradeTableRow). 고유한 룰 이름 텍스트에서
 * TradeTableRow가 항상 갖는 클래스(hover:bg-trade-elevated)를 가진 가장 가까운 상위 div로
 * 거슬러 올라가 정확히 그 한 행만 잡는다(이름 셀이 sibling이므로 ancestor가 행).
 */
function ruleRow(page: Page, name: string) {
  return page
    .getByText(name, { exact: false })
    .first()
    .locator("xpath=ancestor::div[contains(@class,'hover:bg-trade-elevated')][1]");
}

/** 전략 설정: 해당 룰 행의 "활성화"(DRAFT→ACTIVE) 클릭. 이미 ACTIVE면 통과. */
async function activateRule(page: Page, name: string) {
  await page.goto("/trading/paper/rules");
  await expect(page.getByText(name).first()).toBeVisible({ timeout: 15_000 });
  const row = ruleRow(page, name);
  const activateBtn = row.getByRole("button", { name: "활성화" });
  if ((await activateBtn.count()) === 0) {
    return; // 이미 ACTIVE(활성화 버튼 없음)
  }
  await activateBtn.click();
  // 활성화되면 같은 행에 "하향"(ACTIVE→DRAFT) 버튼이 나타난다
  await expect(row.getByRole("button", { name: "하향" })).toBeVisible({ timeout: 15_000 });
}

/**
 * 전략 운영: 해당 룰 "시작"(STOPPED→RUNNING).
 * 폐장(ERR_MARKET_CLOSED) 또는 실시간랭킹 실패 시 RUNNING 생성 불가 — 경고만 남기고 계속.
 * @returns 실제 RUNNING 전환 성공 여부
 */
async function startRule(page: Page, name: string): Promise<boolean> {
  await page.goto("/trading/paper/rules-lifecycle");
  await expect(page.getByText(name).first()).toBeVisible({ timeout: 15_000 });
  const startBtn = ruleRow(page, name).getByRole("button", { name: "시작" });
  if ((await startBtn.count()) === 0) {
    console.warn(`[seed] "${name}" 시작 버튼 없음(이미 실행 중이거나 ACTIVE 아님) — 스킵`);
    return false;
  }
  await startBtn.click();

  // 성공: "실행 중" 배지 / 실패: 폐장·랭킹실패 에러 배너
  const running = page.getByText(/실행 중/).first();
  const errorBanner = page.getByText(/폐장|실시간 거래대금 순위|실패/).first();
  const result = await Promise.race([
    running.waitFor({ state: "visible", timeout: 15_000 }).then(() => "running" as const),
    errorBanner.waitFor({ state: "visible", timeout: 15_000 }).then(() => "error" as const),
  ]).catch(() => "timeout" as const);

  if (result === "running") return true;
  const msg = await errorBanner.textContent().catch(() => null);
  console.warn(`[seed] "${name}" 시작 실패 → RUNNING 예시 생략. 사유: ${msg ?? result}`);
  return false;
}

test.describe("데모 데이터 시드 (전략 설정 + 전략 운영)", () => {
  test.use({ viewport: { width: 1440, height: 900 } });
  // 시드는 직렬로(상태 의존).
  test.describe.configure({ mode: "serial" });

  test("예시 룰 생성·활성화 후 1개 실행", async ({ page }) => {
    test.skip(!SEED_RUN, "SEED_RUN=1 일 때만 실행되는 시더입니다.");
    test.setTimeout(180_000); // 룰 3개 생성+활성화+실행, 넉넉히
    expect(EMAIL, "SEED_EMAIL 환경변수 필요").not.toBe("");
    expect(PASSWORD, "SEED_PASSWORD 환경변수 필요").not.toBe("");

    await login(page);
    await ensurePaperMode(page);

    // 전략 설정: 모든 예시 룰 생성 + 활성화
    for (const rule of DEMO_RULES) {
      await createRule(page, rule);
      await activateRule(page, rule.name);
      console.info(`[seed] 생성+활성화 완료: ${rule.name}`);
    }

    // 전략 운영: 첫 번째 룰만 RUNNING 예시로 시작(나머지는 ACTIVE/STOPPED로 남겨 대비 노출)
    const started = await startRule(page, DEMO_RULES[0]!.name);
    if (started) {
      console.info(`[seed] 실행(RUNNING) 예시 생성: ${DEMO_RULES[0]!.name}`);
    } else {
      console.warn(
        "[seed] RUNNING 예시 미생성(폐장/장외 가능). 평일 09:00–18:00 KST 재실행 시 생성됩니다."
      );
    }

    // 최종 확인: 전략 운영 화면에 예시 룰들이 보인다
    await page.goto("/trading/paper/rules-lifecycle");
    for (const rule of DEMO_RULES) {
      await expect(page.getByText(rule.name).first()).toBeVisible({ timeout: 15_000 });
    }
  });
});
