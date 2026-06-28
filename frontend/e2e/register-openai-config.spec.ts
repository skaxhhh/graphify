import { expect, test } from "@playwright/test";

/**
 * /admin/openai 화면(Azure OpenAI 설정)을 통해 SKAX 게이트웨이 연결 정보를
 * openai_settings DB 에 등록한다. 관리자 계정으로 로그인 → 폼 입력 → 저장.
 *
 * 실행: cd frontend && PLAYWRIGHT_SKIP_WEBSERVER=1 npx playwright test register-openai-config
 * (프론트 5173 / 백엔드 8081 이 떠 있어야 함)
 */

const ADMIN_EMAIL = "admin@graphify.dev";
const ADMIN_PASSWORD = "admin1234";

const CONFIG = {
  endpointUrl: "https://skax.ai-talentlab.com",
  apiKey: "atl-rN00Dl3KRNjR63xQ39tqv2aasowfJGbU2IK0r9SE",
  apiVersion: "2024-12-01-preview",
  deploymentName: "gpt-4.1",
  model: "gpt-4.1",
  embeddingDeployment: "text-embedding-3-large",
  embeddingModel: "text-embedding-3-large",
};

test("register SKAX Azure OpenAI config via admin UI", async ({ page }) => {
  // 1) 관리자 로그인 (vite 프록시 통해 백엔드 호출)
  const loginRes = await page.request.post("/api/v1/auth/login", {
    data: { email: ADMIN_EMAIL, password: ADMIN_PASSWORD },
  });
  expect(loginRes.ok()).toBeTruthy();
  const loginBody = await loginRes.json();
  const auth = loginBody.data;
  expect(auth?.accessToken).toBeTruthy();

  // 2) 토큰을 localStorage 에 주입한 뒤 관리자 페이지 진입
  await page.goto("/");
  await page.evaluate((a) => {
    localStorage.setItem("graphify.accessToken", a.accessToken);
    localStorage.setItem("graphify.refreshToken", a.refreshToken);
    localStorage.setItem("graphify.user", JSON.stringify(a.user));
  }, auth);

  await page.goto("/admin/openai");

  // 3) 폼 입력
  await expect(page.locator("#endpointUrl")).toBeVisible({ timeout: 15_000 });
  await page.locator("#endpointUrl").fill(CONFIG.endpointUrl);
  await page.locator("#apiKey").fill(CONFIG.apiKey);
  await page.locator("#deploymentName").fill(CONFIG.deploymentName);
  await page.locator("#apiVersion").fill(CONFIG.apiVersion);
  await page.locator("#model").selectOption(CONFIG.model);
  await page.locator("#embeddingDeployment").fill(CONFIG.embeddingDeployment);
  await page.locator("#embeddingModel").fill(CONFIG.embeddingModel);

  // 4) 저장
  await page.getByRole("button", { name: "저장", exact: true }).click();

  // 5) 저장 성공 토스트 확인
  await expect(page.getByText("저장되었습니다.")).toBeVisible({ timeout: 15_000 });

  // 6) 저장된 값 재조회 검증
  const configRes = await page.request.get("/api/v1/admin/openai/config", {
    headers: { Authorization: `Bearer ${auth.accessToken}` },
  });
  expect(configRes.ok()).toBeTruthy();
  const configBody = await configRes.json();
  expect(configBody.data.endpointUrl).toBe(CONFIG.endpointUrl);
  expect(configBody.data.deploymentName).toBe(CONFIG.deploymentName);
  expect(configBody.data.apiVersion).toBe(CONFIG.apiVersion);
  expect(configBody.data.model).toBe(CONFIG.model);
  expect(configBody.data.embeddingModel).toBe(CONFIG.embeddingModel);
  expect(configBody.data.hasApiKey).toBe(true);
  expect(configBody.data.configured).toBe(true);
});
