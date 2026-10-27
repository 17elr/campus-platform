const { chromium } = require(
  process.env.PLAYWRIGHT_MODULE ||
    "C:/Users/17elr/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright",
);
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const base = process.env.TEST_BASE_URL || "http://127.0.0.1:5173";
const output = path.resolve(__dirname, "../test-results/admin-content");
const fixtures = [];
const workspaces = {
  LOST: {
    heading: "寻物线索核查",
    layout: ".lost-workbench",
    record: ".lost-record",
  },
  FOUND: {
    heading: "招领物品核验",
    layout: ".found-board",
    record: ".found-record",
  },
  SECOND_HAND: {
    heading: "二手商品审核",
    layout: ".market-catalog",
    record: ".market-record",
  },
};
for (const [index, type] of ["LOST", "FOUND", "SECOND_HAND"].entries()) {
  for (let row = 0; row < 14; row++)
    fixtures.push({
      id: index * 100 + row + 1,
      type,
      title: `${type} 信息 ${row}`,
      description: `物品描述 ${row}`,
      status: ["APPROVED", "PENDING", "HIDDEN", "WITHDRAWN", "DELETED"][
        row % 5
      ],
      location: row % 2 ? "教学楼" : "图书馆",
      category: "电子产品",
      contact: "校内服务台",
      userId: 9,
      createdAt: "2026-10-23T09:30:00",
    });
}

(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    const page = await browser.newPage({
      viewport: { width: 1920, height: 1080 },
    });
    const errors = [],
      writes = [],
      adminReads = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/api/**", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      const isAdmin =
        request.headers().authorization === "Bearer admin-fixture";
      let data = { records: [], total: 0 };
      const send = (value, code = 200, message = "") =>
        route.fulfill({
          contentType: "application/json",
          body: JSON.stringify({ code, data: value, message }),
        });
      if (url.pathname.startsWith("/api/admin/") && !isAdmin)
        return send(null, 403, "无管理员权限");
      if (request.method() === "PUT") {
        assert.match(url.pathname, /^\/api\/admin\/items\/\d+\/status$/);
        const item = fixtures.find(
          (item) => item.id === Number(url.pathname.split("/")[4]),
        );
        assert(item);
        const { status } = request.postDataJSON();
        assert(["APPROVED", "HIDDEN"].includes(status));
        writes.push({ id: item.id, status });
        item.status = status;
        return send(item);
      }
      assert.equal(request.method(), "GET");
      if (url.pathname === "/api/auth/me")
        data = {
          id: 1,
          username: "fixture",
          nickname: "测试账号",
          role: isAdmin ? "ADMIN" : "STUDENT",
        };
      else if (url.pathname === "/api/admin/items") {
        adminReads.push(url);
        const params = url.searchParams,
          scoped = fixtures.filter(
            (item) => !params.get("type") || item.type === params.get("type"),
          );
        const counts = { total: scoped.length };
        for (const status of [
          "PENDING",
          "APPROVED",
          "HIDDEN",
          "WITHDRAWN",
          "DELETED",
        ])
          counts[status] = scoped.filter(
            (item) => item.status === status,
          ).length;
        const records = scoped.filter(
          (item) =>
            (!params.get("status") || item.status === params.get("status")) &&
            (!params.get("location") ||
              item.location === params.get("location")) &&
            (item.title + item.description + item.category).includes(
              params.get("keyword") || "",
            ),
        );
        const size = Number(params.get("size") || 10),
          offset = (Number(params.get("page") || 1) - 1) * size;
        data = {
          records: records.slice(offset, offset + size),
          total: records.length,
          counts,
        };
      } else if (url.pathname === "/api/items") {
        const params = url.searchParams;
        const visible = fixtures.filter((item) => item.status === "APPROVED");
        data = {
          records: visible.filter(
            (item) => !params.get("type") || item.type === params.get("type"),
          ),
          total: visible.length,
          counts: Object.fromEntries(
            ["LOST", "FOUND", "SECOND_HAND"].map((type) => [
              type,
              visible.filter((item) => item.type === type).length,
            ]),
          ),
        };
      } else if (["/api/notices", "/api/advertisements"].includes(url.pathname))
        data = [];
      else if (url.pathname === "/api/admin/stats")
        data = { users: 8, items: fixtures.length, approved: 9, pending: 9 };
      else if (url.pathname === "/api/admin/trade/stats")
        data = { gross: 0, orders: 0, commission: 0, trend: [] };
      else if (url.pathname === "/api/admin/settings") data = {};
      return send(data);
    });
    await page.addInitScript(() => {
      if (!localStorage.getItem("campus_token"))
        localStorage.setItem("campus_token", "admin-fixture");
    });
    const idle = () => page.locator(".content-search:not(:disabled)").waitFor();
    for (const width of [1366, 1920]) {
      await page.setViewportSize({ width, height: 1080 });
      await page.goto(base + "/#admin/items/all");
      await idle();
      for (const [type, label] of [
        ["LOST", "寻物启事"],
        ["FOUND", "失物招领"],
        ["SECOND_HAND", "二手闲置"],
      ]) {
        await page
          .locator(".left>button")
          .filter({ hasText: label + "管理" })
          .click();
        await idle();
        assert.equal(new URL(page.url()).hash, "#admin/items/" + type);
        assert.equal(
          await page.locator(".page-topbar h1").innerText(),
          label + "管理",
        );
        assert.equal(
          await page.locator(".category-management .admin-head h2").innerText(),
          workspaces[type].heading,
        );
        assert.equal(await page.locator(".left>button.active").count(), 1);
        assert.equal(await page.locator(".hall-discovery").count(), 0);
        assert.equal(await page.locator(".grid>.card").count(), 0);
        assert.equal(
          await page
            .locator(".header-actions")
            .getByRole("button", { name: /发布/ })
            .count(),
          0,
        );
        assert.equal(adminReads.at(-1).searchParams.get("type"), type);
        assert.equal(
          await page.locator("[data-summary-total]").innerText(),
          "14",
        );
        assert.equal(await page.locator(workspaces[type].layout).count(), 1);
        for (const [other, workspace] of Object.entries(workspaces)) {
          if (other !== type)
            assert.equal(await page.locator(workspace.layout).count(), 0);
        }
        assert.equal(await page.locator(".content-table").count(), 0);
        assert.equal(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth + 1,
          ),
          false,
        );
        if (width === 1920)
          await page.screenshot({
            path: path.join(output, type.toLowerCase() + ".png"),
            fullPage: true,
          });
      }
      console.log(
        `PASS ${width}px: distinct clue review, item verification cards and product catalog; no overflow`,
      );
    }

    await page
      .locator(".left>button")
      .filter({ hasText: "寻物启事管理" })
      .click();
    await idle();
    await page.getByRole("button", { name: "下一页", exact: true }).click();
    await idle();
    assert.equal(adminReads.at(-1).searchParams.get("page"), "2");
    await page.getByLabel("关键词", { exact: true }).fill("LOST 信息 13");
    await page.getByRole("button", { name: "查询", exact: true }).click();
    await idle();
    assert.equal(adminReads.at(-1).searchParams.get("page"), "1");
    assert.equal(await page.locator(".lost-record").count(), 1);
    assert.match(
      await page.locator(".lost-record").innerText(),
      /LOST 信息 13/,
    );
    await page.getByRole("button", { name: "重置", exact: true }).click();
    await idle();
    await page.getByLabel("所在地点", { exact: true }).fill("教学楼");
    await page.getByLabel("发布状态", { exact: true }).selectOption("PENDING");
    await page.getByRole("button", { name: "查询", exact: true }).click();
    await idle();
    assert.equal(adminReads.at(-1).searchParams.get("location"), "教学楼");
    assert.equal(adminReads.at(-1).searchParams.get("status"), "PENDING");
    await page.locator(".lost-record").first().click();
    const review = page.locator(".lost-review");
    await review.getByRole("button", { name: "查看详情" }).click();
    assert.equal(await page.locator("dialog[open]").count(), 1);
    assert.match(
      await page.locator(".item-inspector").innerText(),
      /校内服务台/,
    );
    await page.keyboard.press("Escape");
    assert.equal(await page.locator("dialog[open]").count(), 0);
    const initialPending = fixtures.filter(
      (item) => item.type === "LOST" && item.status === "PENDING",
    ).length;
    page.once("dialog", (dialog) => dialog.accept());
    await review.getByRole("button", { name: "审核通过" }).click();
    await page.getByRole("status").filter({ hasText: "信息已公开" }).waitFor();
    assert.equal(writes.at(-1).status, "APPROVED");
    assert.equal(
      Number(await page.locator('[data-summary-state="PENDING"]').innerText()),
      initialPending - 1,
    );
    await page.getByRole("button", { name: "重置", exact: true }).click();
    await idle();
    const beforeCancel = writes.length;
    page.once("dialog", (dialog) => dialog.dismiss());
    await page
      .getByRole("button", { name: "下架", exact: true })
      .first()
      .click();
    assert.equal(writes.length, beforeCancel);
    page.once("dialog", (dialog) => dialog.accept());
    await page
      .getByRole("button", { name: "下架", exact: true })
      .first()
      .click();
    await page.getByRole("status").filter({ hasText: "信息已下架" }).waitFor();
    assert.equal(writes.at(-1).status, "HIDDEN");
    page.once("dialog", (dialog) => dialog.accept());
    await page
      .getByRole("button", { name: "恢复公开", exact: true })
      .first()
      .click();
    await page.getByRole("status").filter({ hasText: "信息已公开" }).waitFor();
    assert.equal(writes.at(-1).status, "APPROVED");
    await page.locator(".lost-record").nth(1).focus();
    await page.keyboard.press("Enter");
    assert.equal(
      await page.locator(".lost-record").nth(1).getAttribute("aria-pressed"),
      "true",
    );
    assert.equal(
      await review.locator(".review-caption b").innerText(),
      "#" +
        (await page
          .locator(".lost-record")
          .nth(1)
          .getAttribute("data-record-id")),
    );
    for (const [type, label] of [
      ["FOUND", "失物招领"],
      ["SECOND_HAND", "二手闲置"],
    ]) {
      await page
        .locator(".left>button")
        .filter({ hasText: label + "管理" })
        .click();
      await idle();
      await page
        .getByLabel("发布状态", { exact: true })
        .selectOption("PENDING");
      await page.getByRole("button", { name: "查询", exact: true }).click();
      await idle();
      const record = page.locator(workspaces[type].record).first();
      const id = Number(await record.getAttribute("data-record-id"));
      await record.getByRole("button", { name: "查看详情" }).click();
      assert.match(
        await page.locator(".item-inspector").innerText(),
        new RegExp(type),
      );
      await page.getByRole("button", { name: "关闭管理详情" }).click();
      page.once("dialog", (dialog) => dialog.accept());
      await record.getByRole("button", { name: "审核通过" }).click();
      await page
        .getByRole("status")
        .filter({ hasText: "信息已公开" })
        .waitFor();
      assert.deepEqual(writes.at(-1), { id, status: "APPROVED" });
      const summaryButton = page
        .locator('[data-summary-state="PENDING"]')
        .locator("xpath=..");
      if (type === "FOUND") {
        assert.equal(await summaryButton.getAttribute("aria-pressed"), "true");
      } else {
        await page.getByRole("button", { name: /公开在架/ }).click();
        await idle();
        assert.equal(adminReads.at(-1).searchParams.get("status"), "APPROVED");
      }
    }
    console.log(
      "PASS server query parameters, pagination reset, detail dialog, approve/hide/restore and cancelled action",
    );

    await page
      .locator(".left>button")
      .filter({ hasText: "寻物启事管理" })
      .click();
    await idle();
    await page.reload();
    await idle();
    assert.equal(await page.locator(".category-management").count(), 1);
    assert.equal(adminReads.at(-1).searchParams.get("type"), "LOST");
    await page
      .locator(".left>button")
      .filter({ hasText: "失物招领管理" })
      .click();
    await idle();
    await page.goBack();
    await idle();
    assert.equal(adminReads.at(-1).searchParams.get("type"), "LOST");
    await page.goto(base + "/#hall");
    await idle();
    assert.equal(new URL(page.url()).hash, "#admin/items/all");
    console.log("PASS refresh, browser back and admin public-hall redirect");

    await page.evaluate(() =>
      localStorage.setItem("campus_token", "student-fixture"),
    );
    const readCount = adminReads.length;
    await page.goto(base + "/#admin/items/LOST");
    await page.reload();
    await page
      .locator(".header-actions")
      .getByRole("button", { name: "退出登录" })
      .waitFor();
    assert.equal(await page.locator(".category-management").count(), 0);
    assert.equal(adminReads.length, readCount);
    await page.locator(".left>button").filter({ hasText: "寻物启事" }).click();
    await page.locator(".hall-discovery").waitFor({ state: "visible" });
    assert.equal(await page.locator(".content-records").count(), 0);
    assert.equal(await page.locator(".discovery-publish").isVisible(), true);
    assert.deepEqual(errors, []);
    console.log(
      "PASS student route protection and unchanged public browsing; no runtime errors",
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
