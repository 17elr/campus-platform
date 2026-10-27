// Requires the running site and services; uses only read-only APIs and a login session.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const assert = require("node:assert/strict");

(async () => {
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    const page = await browser.newPage();
    const errors = [];
    const apiUrls = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("request", (request) => {
      if (new URL(request.url()).pathname.startsWith("/api/"))
        apiUrls.push(request.url());
    });
    const notices = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/notices") && response.status() === 200,
    );
    await page.goto(process.env.TEST_BASE_URL || "http://127.0.0.1:5173");
    await notices;
    const result = await page.evaluate(async () => {
      const response = await fetch("http://127.0.0.1:8088/api/delivery/public");
      return response.json();
    });
    assert.equal(result.code, 200);
    assert.ok(apiUrls.length > 0);
    assert.ok(
      apiUrls.every((url) => new URL(url).port === "8088"),
      "Frontend bypassed the gateway",
    );
    assert.deepEqual(errors, []);
    console.log(
      "PASS browser: homepage, notice CORS, public delivery, all API requests through gateway",
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
