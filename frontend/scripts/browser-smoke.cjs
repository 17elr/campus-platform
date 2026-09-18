// Run with Playwright installed, or PLAYWRIGHT_MODULE pointing to a local installation.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const out = path.resolve(__dirname, '../test-results')
fs.mkdirSync(out, { recursive: true })

;(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const results = []
  const check = async (name, fn) => { await fn(); results.push(name); console.log(`PASS ${name}`) }
  const base = process.env.TEST_BASE_URL || 'http://localhost:5173'
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const page = await context.newPage()
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    await check('Actual homepage opens anonymously with styled layout', async () => {
      await page.goto(base)
      await page.getByRole('heading', { name: '让物品找到归属， 让好物继续相遇。' }).waitFor()
      assert.equal(await page.locator('dialog[open]').count(), 0)
      assert.equal(await page.locator('.hero').evaluate(e => getComputedStyle(e).display), 'flex')
      await page.screenshot({ path: path.join(out, 'home-actual.png'), fullPage: true })
    })
    const records = [
      { id: 1, title: '图书馆拾到校园卡', description: '在二楼阅览室拾到，请携带有效证明认领。', type: 'FOUND', location: '图书馆', category: '证件', contact: '图书馆服务台', createdAt: '2026-09-12T10:00:00' },
      { id: 2, title: '寻找蓝色保温杯', description: '周五下午遗落在教学楼，杯底有姓名贴纸。', type: 'LOST', location: '教学楼', category: '生活用品', contact: '测试联系方式', createdAt: '2026-09-12T09:00:00' },
      { id: 3, title: '转让考研英语资料', description: '书籍保存良好，校内当面查看。', type: 'SECOND_HAND', location: '学生宿舍', category: '书籍', contact: '测试联系方式', createdAt: '2026-09-11T09:00:00' },
    ]
    let failList = false, allowLogin = false, posts = 0
    await page.route('**/api/**', async route => {
      const req = route.request(), url = new URL(req.url())
      const send = body => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
      if (url.pathname.endsWith('/items') && req.method() === 'GET') {
        if (failList) return route.abort()
        const keyword = url.searchParams.get('keyword') || '', type = url.searchParams.get('type') || ''
        return send(records.filter(x => (!type || x.type === type) && (x.title + x.description).includes(keyword)))
      }
      if (url.pathname.endsWith('/auth/login')) return send(allowLogin ? { code: 200, data: { token: 'test', user: { id: 2, username: 'student', nickname: '测试用户', role: 'STUDENT' } } } : { code: 400, message: '账号或密码错误', data: null })
      if (url.pathname.endsWith('/items') && req.method() === 'POST') { posts++; assert.equal(req.headers()['x-user-id'], '2'); return send({ id: 4, ...req.postDataJSON() }) }
      return send({ code: 200, data: {} })
    })
    await check('Guest reads lists, searches and opens details (API fixtures)', async () => {
      await page.reload()
      await page.locator('.card').first().waitFor()
      assert.equal(await page.locator('.card').count(), 3)
      await page.getByRole('searchbox').fill('校园卡')
      await page.getByRole('button', { name: '搜索信息', exact: true }).click()
      await page.waitForFunction(() => document.querySelectorAll('.card').length === 1)
      await page.getByRole('button', { name: '查看详情 →' }).click()
      await page.getByRole('heading', { name: '图书馆拾到校园卡' }).last().waitFor()
      assert.equal(await page.locator('.auth-dialog').count(), 0)
      await page.getByRole('button', { name: '关闭详情' }).click()
    })
    await check('Category filters and empty results work without login', async () => {
      await page.getByRole('searchbox').fill('')
      await page.getByRole('navigation').getByRole('button', { name: '二手闲置', exact: true }).click()
      await page.getByRole('heading', { name: '转让考研英语资料' }).waitFor()
      await page.getByRole('searchbox').fill('不存在的信息')
      await page.getByRole('button', { name: '搜索信息', exact: true }).click()
      await page.getByRole('heading', { name: '暂时没有相关信息' }).waitFor()
    })
    await check('Publish requires optional login; dismiss preserves browsing state', async () => {
      await page.locator('.header-actions').getByRole('button', { name: '发布信息' }).click()
      await page.getByRole('heading', { name: '欢迎回到校园互助' }).waitFor()
      assert.equal(posts, 0)
      await page.getByRole('button', { name: '继续浏览' }).click()
      assert.equal(await page.locator('dialog[open]').count(), 0)
      assert.equal(await page.getByRole('searchbox').inputValue(), '不存在的信息')
      await page.locator('.header-actions').getByRole('button', { name: '注册', exact: true }).click()
      await page.getByRole('heading', { name: '加入校园互助' }).waitFor()
      await page.getByRole('button', { name: '继续浏览' }).click()
    })
    await check('HTTP-200 login rejection never creates a user session', async () => {
      await page.locator('.header-actions').getByRole('button', { name: '发布信息' }).click()
      await page.getByLabel('用户名', { exact: true }).fill('student')
      await page.getByLabel('密码', { exact: true }).fill('wrong')
      await page.locator('.auth-dialog').getByRole('button', { name: '登录', exact: true }).click()
      await page.getByRole('alert').filter({ hasText: '账号或密码错误' }).waitFor()
      assert.equal(await page.evaluate(() => localStorage.getItem('user')), null)
    })
    await check('Successful test login resumes publishing; logout returns to public homepage', async () => {
      allowLogin = true
      await page.locator('.auth-dialog').getByRole('button', { name: '登录', exact: true }).click()
      await page.getByRole('heading', { name: '发布校园信息' }).waitFor()
      await page.getByLabel('标题', { exact: true }).fill('测试发布')
      await page.getByLabel('地点', { exact: true }).fill('图书馆')
      await page.getByLabel('联系方式', { exact: true }).fill('服务台')
      await page.getByLabel('详细描述', { exact: true }).fill('浏览器交互测试，不写入真实数据库。')
      await page.getByRole('button', { name: '提交审核', exact: true }).click()
      await page.getByRole('status').filter({ hasText: '发布成功' }).waitFor()
      assert.equal(posts, 1)
      await page.getByRole('button', { name: '退出登录' }).click()
      assert.equal(await page.locator('dialog[open]').count(), 0)
      assert.equal(await page.getByRole('button', { name: '管理中心' }).count(), 0)
    })
    await check('API failure has visible retry and successful recovery', async () => {
      failList = true
      await page.getByRole('searchbox').fill('')
      await page.getByRole('navigation').getByRole('button', { name: '全部信息', exact: true }).click()
      await page.getByRole('heading', { name: '信息暂时未能加载' }).waitFor()
      failList = false
      await page.getByRole('button', { name: '重新加载' }).click()
      await page.locator('.card').first().waitFor()
      await page.screenshot({ path: path.join(out, 'home-fixtures.png'), fullPage: true })
    })
    await check('Mobile browsing and login dialog fit 390px viewport', async () => {
      await page.setViewportSize({ width: 390, height: 844 })
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
      await page.screenshot({ path: path.join(out, 'mobile-fixtures.png'), fullPage: true })
      await page.locator('.header-actions').getByRole('button', { name: '发布信息' }).click()
      const box = await page.locator('.auth-dialog').boundingBox()
      assert(box.x >= 0 && box.x + box.width <= 390)
      await page.getByRole('button', { name: '继续浏览' }).click()
    })
    await check('Malformed saved user state cannot break guest homepage', async () => {
      await page.evaluate(() => localStorage.setItem('user', '{invalid'))
      await page.reload()
      await page.getByRole('heading', { name: '让物品找到归属， 让好物继续相遇。' }).waitFor()
      assert.equal(await page.locator('dialog[open]').count(), 0)
    })
    assert.deepEqual(errors, [])
    fs.writeFileSync(path.join(out, 'browser-report.json'), JSON.stringify({ passed: results, uncaughtErrors: errors, note: 'UI integration uses intercepted API fixtures. Does not verify real backend authentication or MySQL persistence.' }, null, 2))
  } finally { await browser.close() }
})().catch(error => { console.error(error); process.exitCode = 1 })
