const { chromium } = require('C:/Users/17elr/AppData/Local/campus-browser-check/node_modules/playwright')
const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')

const site = 'http://127.0.0.1:5173/'
const api = 'http://127.0.0.1:8080/api'
const marker = `验收${Date.now()}`
const campusId = `QA${Date.now()}`
const results = []
const ids = []

async function token(username) {
  const response = await fetch(`${api}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password: username === 'admin' ? 'admin123' : 'demo12345' }) })
  const result = await response.json()
  assert.equal(result.code, 200, `${username} 登录失败：${result.message}`)
  return result.data.token
}

async function pageFor(browser, username, section) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  await context.addInitScript(value => localStorage.setItem('campus_token', value), await token(username))
  const page = await context.newPage()
  await page.goto(site)
  await page.getByRole('button', { name: section, exact: true }).click()
  return { page, context }
}

async function orderByMarker(page, suffix) {
  const row = page.locator('.order-row').filter({ hasText: `${marker}-${suffix}` })
  await row.waitFor()
  const text = await row.textContent()
  const match = text.match(/#(\d+)/)
  assert.ok(match, `无法读取 ${suffix} 订单编号`)
  const id = Number(match[1])
  if (!ids.includes(id)) ids.push(id)
  return row
}

async function createOrder(page, suffix) {
  await page.getByRole('button', { name: '发布配送需求', exact: true }).first().click()
  await page.getByPlaceholder('例如：东门快递站').fill(`${marker}-${suffix}取货点`)
  await page.getByPlaceholder('例如：3号宿舍楼下').fill(`${marker}-${suffix}送达点`)
  await page.getByPlaceholder('取件码、商品信息或其他说明').fill(`${marker}-${suffix}`)
  await page.getByRole('button', { name: '确认发布' }).click()
  await orderByMarker(page, suffix)
  results.push(`${suffix} 发布配送需求：通过`)
}

async function run() {
  const browser = await chromium.launch({ headless: true, executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' })
  const contexts = []
  const errors = []
  try {
    const courier = await pageFor(browser, 'demo2', '校园配送'); contexts.push(courier.context)
    courier.page.on('pageerror', error => errors.push(`配送员：${error.message}`))
    await courier.page.getByRole('button', { name: '申请配送员' }).click()
    await courier.page.getByPlaceholder('学号或校内工号').fill(campusId)
    await courier.page.getByPlaceholder('配送时段或校区').fill(marker)
    await courier.page.getByRole('button', { name: '提交申请' }).click()
    await courier.page.getByText('申请已提交，等待管理员审核。').first().waitFor()
    results.push('配送员申请：通过，显示待审核')
    await courier.page.getByRole('button', { name: /可接订单.*待认证/ }).click()
    assert.equal(await courier.page.getByRole('button', { name: '接单', exact: true }).count(), 0)
    results.push('未审核接单大厅：通过，无法进入或接单')

    const admin = await pageFor(browser, 'admin', '管理员后台'); contexts.push(admin.context)
    admin.page.on('pageerror', error => errors.push(`管理员：${error.message}`))
    await admin.page.getByRole('button', { name: '配送员审核' }).click()
    const application = admin.page.locator('.review-row').filter({ hasText: marker })
    await application.waitFor()
    admin.page.once('dialog', dialog => dialog.accept('身份核验通过'))
    await application.getByRole('button', { name: '通过' }).click()
    await application.getByText('已通过').waitFor()
    results.push('管理员审核：通过，申请状态变为已通过')

    await courier.page.reload()
    await courier.page.getByRole('button', { name: '校园配送', exact: true }).click()
    await courier.page.getByRole('button', { name: '可接订单', exact: true }).waitFor()
    results.push('审核后接单大厅：通过，入口开放')

    const requester = await pageFor(browser, 'demo1', '校园配送'); contexts.push(requester.context)
    requester.page.on('pageerror', error => errors.push(`发布人：${error.message}`))

    for (const branch of ['恢复', '取消']) {
      await createOrder(requester.page, branch)
      await courier.page.getByRole('button', { name: '可接订单', exact: true }).click()
      await courier.page.getByRole('button', { name: '刷新' }).click()
      const available = courier.page.locator('.order-row').filter({ hasText: `${marker}-${branch}取货点` })
      await available.waitFor()
      await available.getByRole('button', { name: '接单' }).click()
      await courier.page.getByText('接单成功。').waitFor()
      results.push(`${branch}分支接单大厅与抢单：通过`)

      await requester.page.getByRole('button', { name: '刷新' }).click()
      const own = await orderByMarker(requester.page, branch)
      assert.match(await own.textContent(), /已接单/)
      requester.page.once('dialog', dialog => dialog.accept(`${marker}-${branch}异常`))
      await own.getByRole('button', { name: '报告异常' }).click()
      await own.getByText('异常处理中').waitFor()
      results.push(`${branch}分支订单异常上报：通过`)

      await admin.page.getByRole('button', { name: '配送管理' }).click()
      await admin.page.getByRole('button', { name: '刷新' }).last().click()
      const managed = admin.page.locator('.order-row').filter({ hasText: `${marker}-${branch}` })
      await managed.waitFor()
      admin.page.once('dialog', dialog => dialog.accept(`${marker}-${branch}处理`))
      await managed.getByRole('button', { name: branch === '恢复' ? '恢复配送' : '取消配送' }).click()
      await managed.getByText(branch === '恢复' ? '已接单' : '已取消').waitFor()
      results.push(`管理员${branch}配送：通过`)
      await admin.page.getByRole('button', { name: '配送员审核' }).click()
      await requester.page.getByRole('button', { name: '刷新' }).click()
      assert.match(await own.textContent(), branch === '恢复' ? /已接单/ : /已取消/)
      results.push(`发布人查看${branch}处理结果：通过`)
    }
    assert.deepEqual(errors, [])
    console.log(JSON.stringify({ marker, orderIds: ids, results, errors }, null, 2))
  } finally {
    await Promise.all(contexts.map(context => context.close()))
    await browser.close()
    const sql = `${ids.length ? `DELETE FROM delivery_order WHERE id IN (${ids.join(',')}) AND requester_note LIKE '${marker}-%' AND receiver_id=7;` : ''}DELETE FROM delivery_courier_application WHERE user_id=8 AND campus_id='${campusId}';`
    const cleanup = spawnSync('mysql', ['-uroot', '-D', 'campus_platform', '-e', sql], { encoding: 'utf8', env: { ...process.env, MYSQL_PWD: process.env.DB_PASSWORD || '123456' } })
    if (cleanup.status !== 0) console.error(`测试数据清理失败：${cleanup.stderr}`)
  }
}

run().catch(error => { console.error(JSON.stringify({ marker, orderIds: ids, results, error: error.message }, null, 2)); process.exitCode = 1 })
