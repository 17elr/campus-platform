const { chromium } = require(
  process.env.PLAYWRIGHT_MODULE ||
    "C:/Users/17elr/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright",
);
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const output = path.resolve(__dirname, "../test-results/top-ui");
const base = process.env.TEST_BASE_URL || "http://127.0.0.1:5173";
const entries = [
  {
    id: 1,
    type: "LOST",
    title: "寻找蓝色耳机",
    description: "遗落在图书馆阅览室。",
    location: "图书馆",
    category: "电子产品",
    createdAt: "2026-10-23T09:30:00",
  },
  {
    id: 2,
    type: "FOUND",
    title: "教学楼拾到校园卡",
    description: "请描述卡片特征后认领。",
    location: "教学楼",
    category: "证件",
    createdAt: "2026-10-23T09:00:00",
  },
  {
    id: 3,
    type: "SECOND_HAND",
    title: "闲置教材转让",
    description: "书籍保存完好，校内当面查看。",
    location: "学生宿舍",
    category: "书籍",
    createdAt: "2026-10-23T08:00:00",
  },
];

(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    const page = await browser.newPage();
    const errors = [];
    let role = "STUDENT";
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/api/**", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      assert.equal(
        request.method(),
        "GET",
        "UI checks must not mutate server data",
      );
      let data = { records: [], total: 0 };
      if (url.pathname === "/api/items") {
        const params = url.searchParams;
        const records = entries.filter(
          (item) =>
            ["type", "location", "category"].every(
              (key) => !params.get(key) || item[key] === params.get(key),
            ) &&
            (item.title + item.description).includes(
              params.get("keyword") || "",
            ),
        );
        data = {
          records,
          total: records.length,
          counts: { LOST: 1, FOUND: 1, SECOND_HAND: 1 },
        };
      } else if (url.pathname === "/api/auth/me")
        data = { id: 7, username: "ui_test", nickname: "测试同学", role };
      else if (
        url.pathname === "/api/notices" ||
        url.pathname === "/api/advertisements"
      )
        data = [];
      else if (url.pathname === "/api/admin/stats")
        data = { users: 12, items: 3, approved: 3, pending: 0 };
      else if (url.pathname === "/api/admin/trade/stats")
        data = { orders: 0, gross: 0, commission: 0, visitors: 0, trend: [] };
      else if (url.pathname === "/api/admin/settings") data = {};
      else if (url.pathname === "/api/delivery/courier/me")
        data = { status: "NONE" };
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({ code: 200, data }),
      });
    });
    const navigateCategory = (label) =>
      page.locator(".left>button").filter({ hasText: label }).click();
    const capture = async (name) => {
      const main = await page.locator("main").boundingBox();
      await page.screenshot({
        path: path.join(output, name + ".png"),
        clip: { x: main.x, y: 0, width: main.width, height: 660 },
      });
    };
    const titles = [
      [
        "全部信息",
        "ALL",
        "你关心的校园小事，在这里找到答案。",
        ".discovery-channel",
      ],
      ["寻物启事", "LOST", "别着急，让更多人帮你一起找。", ".discovery-steps"],
      ["失物招领", "FOUND", "你拾到的，也许是别人正在找的。", ".claim-ticket"],
      [
        "二手闲置",
        "SECOND_HAND",
        "让闲置流动，让喜欢继续。",
        ".market-summary",
      ],
    ];
    for (const width of [1366, 1920, 2560]) {
      await page.setViewportSize({ width, height: 1000 });
      await page.goto(base);
      for (const [label, kind, title, panel] of titles) {
        await navigateCategory(label);
        await page.waitForFunction(
          (expected) =>
            document.querySelector(".hall-discovery")?.dataset.kind ===
            expected,
          kind,
        );
        await page.locator(".discovery-submit:not(:disabled)").waitFor();
        assert.equal(await page.locator(".page-topbar h1").innerText(), label);
        assert.equal(await page.locator("#hall-heading").innerText(), title);
        assert.equal(await page.locator(panel).first().isVisible(), true);
        assert.equal(await page.locator(".hall-legacy").isVisible(), false);
        assert.equal(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth + 1,
          ),
          false,
          `${width}/${kind} horizontal overflow`,
        );
        const geometry = await page
          .locator(".hall-discovery")
          .evaluate((element) => {
            const box = element.getBoundingClientRect();
            return [...element.querySelectorAll("input, select, button")]
              .filter((control) => control.getBoundingClientRect().width)
              .every((control) => {
                const rect = control.getBoundingClientRect();
                return rect.left >= box.left && rect.right <= box.right + 1;
              });
          });
        assert.equal(geometry, true, `${width}/${kind} clipped controls`);
        if (width === 1920) await capture(kind.toLowerCase());
      }
      console.log(
        `PASS ${width}px: four distinct category headers and visible controls`,
      );
    }

    await navigateCategory("寻物启事");
    const keyword = page.getByRole("searchbox", { name: "搜索校园信息" });
    await keyword.fill("蓝色");
    await page.waitForFunction(
      () => document.querySelectorAll(".grid>.card").length === 1,
    );
    await page
      .locator(".discovery-shortcuts")
      .getByRole("button", { name: "图书馆", exact: true })
      .click();
    assert.equal(await page.getByLabel("所在地点").inputValue(), "图书馆");
    await page.getByLabel("物品类别", { exact: true }).fill("电子产品");
    await page.locator(".discovery-submit:not(:disabled)").waitFor();
    const searchResponse = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        url.pathname === "/api/items" &&
        url.searchParams.get("keyword") === "蓝色" &&
        url.searchParams.get("location") === "图书馆" &&
        url.searchParams.get("category") === "电子产品"
      );
    });
    await keyword.press("Enter");
    await searchResponse;
    await page.getByRole("button", { name: "清除筛选", exact: true }).click();
    assert.equal(await keyword.inputValue(), "");
    assert.equal(await page.getByLabel("所在地点").inputValue(), "");
    assert.equal(
      await page.getByLabel("物品类别", { exact: true }).inputValue(),
      "",
    );
    await page
      .getByRole("button", { name: "发布寻物启事", exact: true })
      .click();
    assert.equal(await page.locator(".login-dialog").isVisible(), true);
    await page.getByRole("button", { name: "关闭登录", exact: true }).click();
    console.log(
      "PASS keyword, location, category, Enter search, reset and guest login",
    );

    await page.evaluate(() => localStorage.setItem("campus_token", "ui-test"));
    await page.reload();
    await page
      .locator(".header-actions")
      .getByRole("button", { name: "退出登录" })
      .waitFor();
    await navigateCategory("失物招领");
    await page
      .getByRole("button", { name: "发布招领信息", exact: true })
      .click();
    assert.equal(
      await page.locator(".dialog select").first().inputValue(),
      "FOUND",
    );
    await page.getByRole("button", { name: "关闭发布窗口" }).click();
    for (const label of ["校园配送", "个人中心"]) {
      await page.locator(".left>button").filter({ hasText: label }).click();
      assert.equal(await page.locator(".page-topbar h1").innerText(), label);
    }

    role = "ADMIN";
    await page.evaluate(() => {
      window.location.hash = "home";
    });
    await page.reload();
    await page.locator(".admin-head").waitFor();
    assert.equal(
      await page.locator(".admin-head h2").innerText(),
      "运营数据总览",
    );
    await page.setViewportSize({ width: 1920, height: 1000 });
    await page
      .locator(".left>button")
      .filter({ hasText: "管理员后台" })
      .click();
    assert.equal(
      await page.locator(".admin-head h2").innerText(),
      "内容审核工作台",
    );
    await page
      .locator(".admin-nav")
      .getByRole("button", { name: /用户列表/ })
      .click();
    assert.equal(
      await page.locator(".admin-head h2").innerText(),
      "用户与账号管理",
    );
    await capture("admin");
    await page
      .locator(".admin-nav")
      .getByRole("button", { name: /平台配置/ })
      .click();
    assert.equal(
      await page.locator(".admin-head h2").innerText(),
      "平台费用与规则",
    );
    assert.deepEqual(errors, []);
    console.log(
      "PASS publish type, page headings, admin section headings; no runtime errors",
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
