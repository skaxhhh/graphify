import { expect, test } from "@playwright/test";

const API_BASE = process.env.PLAYWRIGHT_API_BASE_URL ?? "http://localhost:8081";

test.describe("S07 기업 상세 수집 데이터 표시", () => {
  test.use({ viewport: { width: 1440, height: 900 } });

  test("2열 레이아웃·재무·공시·뉴스·신호·인사이트가 노출된다", async ({ page, request }) => {
    const resolve = await request.post(`${API_BASE}/api/v1/companies/resolve`, {
      data: { query: "삼성전자", ticker: "005930" },
    });
    expect(resolve.ok()).toBeTruthy();
    const companyId = (await resolve.json()).data.id as number;

    const sync = await request.post(`${API_BASE}/api/v1/companies/${companyId}/sync`, {
      data: {},
    });
    expect(sync.ok(), `sync failed: ${await sync.text()}`).toBeTruthy();

    const generate = await request.post(
      `${API_BASE}/api/v1/companies/${companyId}/insights/generate`,
      { data: {} }
    );
    expect(generate.ok(), `generate failed: ${await generate.text()}`).toBeTruthy();

    const detailBody = await (await request.get(`${API_BASE}/api/v1/companies/${companyId}`)).json();
    const profile = detailBody.data.dartProfile;

    expect(profile?.financialStatements?.length).toBeGreaterThan(0);

    await page.goto(`/companies/${companyId}`);

    const layout = page.getByTestId("company-detail-layout");
    await expect(layout).toBeVisible({ timeout: 60_000 });

    const left = page.getByTestId("company-detail-left");
    const right = page.getByTestId("company-detail-right");
    await expect(left).toBeVisible();
    await expect(right).toBeVisible();

    const leftBox = await left.boundingBox();
    const rightBox = await right.boundingBox();
    expect(leftBox && rightBox && leftBox.x < rightBox.x).toBeTruthy();

    await expect(left.getByTestId("financial-section")).toBeVisible();
    await expect(left.getByRole("cell", { name: "매출액" }).first()).toBeVisible();

    const accountCell = left.getByRole("cell", { name: "매출액" }).first();
    const accountBox = await accountCell.boundingBox();
    expect(accountBox && accountBox.width > 40).toBeTruthy();

    if (profile.recentDisclosures.length > 0) {
      await expect(left.getByTestId("disclosure-section")).toBeVisible();
    }
    await expect(left.getByRole("heading", { name: "데이터 출처" })).toBeVisible();

    if (profile.relatedNews.length > 0) {
      await expect(right.getByTestId("news-section")).toBeVisible();
    }
    await expect(right.getByTestId("risk-signals-section")).toBeVisible();
    await expect(right.getByTestId("opportunity-signals-section")).toBeVisible();
    const riskText = await right.getByTestId("risk-signals-section").innerText();
    expect(riskText).not.toContain("AI 메모리 수요 둔화");
    await expect(
      right.getByTestId("agent-insight-section").or(right.getByTestId("agent-insight-loading"))
    ).toBeVisible({ timeout: 15_000 });
    await expect(right.getByTestId("agent-insight-section")).toBeVisible({ timeout: 120_000 });
  });
});
