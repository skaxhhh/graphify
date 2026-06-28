import { expect, test } from "@playwright/test";

/**
 * /trading 챗 화면이 실제 백엔드(/api/v1/trading/chat → DB 등록 Azure OpenAI)와
 * 연동되어 사용자 메시지에 대한 Agent 응답을 렌더링하는지 검증.
 *
 * 실행: cd frontend && PLAYWRIGHT_SKIP_WEBSERVER=1 npx playwright test trading-chat
 */
test("trading chat returns a live agent reply", async ({ page }) => {
  const loginRes = await page.request.post("/api/v1/auth/login", {
    data: { email: "demo@graphify.dev", password: "password123" },
  });
  expect(loginRes.ok()).toBeTruthy();
  const auth = (await loginRes.json()).data;

  await page.goto("/");
  await page.evaluate((a) => {
    localStorage.setItem("graphify.accessToken", a.accessToken);
    localStorage.setItem("graphify.refreshToken", a.refreshToken);
    localStorage.setItem("graphify.user", JSON.stringify(a.user));
  }, auth);

  await page.goto("/trading");

  const input = page.getByPlaceholder(/메시지를 입력하세요/);
  await expect(input).toBeVisible({ timeout: 15_000 });
  await input.fill("간단히 인사만 한 문장으로 해줘.");
  await input.press("Enter");

  // welcome(1) + user(1) + assistant(1) 메시지 버블이 나타날 때까지 대기.
  // 'D' 아바타가 붙는 assistant 버블 텍스트가 2개 이상이면 응답 수신.
  await expect
    .poll(
      async () => {
        const texts = await page
          .locator(".whitespace-pre-wrap")
          .allInnerTexts();
        return texts.length;
      },
      { timeout: 30_000 }
    )
    .toBeGreaterThanOrEqual(3);

  const bubbles = await page.locator(".whitespace-pre-wrap").allInnerTexts();
  const lastReply = bubbles[bubbles.length - 1].trim();
  expect(lastReply.length).toBeGreaterThan(0);
  // 백엔드 미연동 시 떴던 mock/에러 문구가 아니어야 함
  expect(lastReply).not.toContain("백엔드 연동 준비 중");
  expect(lastReply).not.toContain("연결에 실패");
  expect(lastReply).not.toContain("응답을 생성하지 못했습니다");
  console.log("Agent reply:", lastReply);
});
