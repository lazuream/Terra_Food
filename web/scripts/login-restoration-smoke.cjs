// Loopback only; API responses are intercepted, so these checks never change real accounts.
const { chromium } = require('playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const base = process.env.LOGIN_TEST_URL || 'http://127.0.0.1:5173'
const output = process.env.LOGIN_TEST_OUTPUT || path.resolve(__dirname, '../../.local/login-restoration')
assert(['127.0.0.1', 'localhost'].includes(new URL(base).hostname))
fs.mkdirSync(output, { recursive: true })
const user = { id: 901, username: 'login_fixture', displayName: '登录验证', role: 'USER', active: true }
const results = []

async function fixture(browser, options = {}) {
  const context = await browser.newContext({ locale: 'zh-CN', colorScheme: 'light', viewport: { width: options.width || 1704, height: 952 } })
  const page = await context.newPage()
  page.setDefaultTimeout(12000)
  const state = { user: options.remembered ? user : null, meStatus: 200, loginStatus: 200, logoutStatus: 200, calls: [], errors: [], holdLogin: null }
  page.on('pageerror', error => state.errors.push(error.message))
  await page.route('**/*', async route => {
    const request = route.request(), url = new URL(request.url()), p = url.pathname
    if (p.startsWith('/api/')) {
      state.calls.push({ path: p, method: request.method(), headers: request.headers(), data: request.postDataJSON() })
      if (p === '/api/auth/me') return route.fulfill(state.meStatus !== 200 ? { status: state.meStatus, json: {} } : state.user ? { json: state.user } : { status: 401, json: {} })
      if (p === '/api/auth/csrf') return route.fulfill({ json: { token: 'login-fixture-csrf', headerName: 'X-CSRF-TOKEN' } })
      if (p === '/api/auth/login') {
        if (state.holdLogin) await state.holdLogin
        if (state.loginStatus !== 200) return route.fulfill({ status: state.loginStatus, json: {} }).catch(() => {})
        state.user = { ...user, role: request.postDataJSON().role }
        return route.fulfill({ json: state.user }).catch(() => {})
      }
      if (p === '/api/auth/logout') {
        if (state.logoutStatus === 200) state.user = null
        return route.fulfill({ status: state.logoutStatus, json: {} })
      }
      if (p === '/api/auth/register') return route.fulfill({ status: 201, json: user })
      if (p === '/api/foods/catalog') return route.fulfill({ json: { items: [], total: 0, page: 1, pageSize: 20 } })
      if (p === '/api/foods/map-clusters') return route.fulfill({ json: { items: [], total: 0, dataVersion: 1, zoom: 4 } })
      return route.fulfill({ json: [] })
    }
    if (p === '/audio/music-manifest.json') return route.fulfill({ json: [{ name: 'Fixture', artist: 'Test', src: '/audio/fixture.mp3' }] })
    if (url.origin !== new URL(base).origin || p === '/audio/fixture.mp3') return route.abort()
    return route.continue()
  })
  return { page, context, state }
}

async function openForm(page) {
  await page.locator('.login-awaken-button').click()
  await page.getByRole('dialog').waitFor()
}
async function submit(page) {
  await page.locator('.login-form input[autocomplete=username]').fill('login_fixture')
  await page.locator('.login-form input[type=password]').fill('Test123456')
  await page.locator('.login-form .login-submit').click()
}

;(async () => {
  const browser = await chromium.launch({ headless: true, ...(process.env.BROWSER_EXECUTABLE ? { executablePath: process.env.BROWSER_EXECUTABLE } : {}) })
  async function run(name, options, body) {
    if (process.env.LOGIN_CASE && name !== process.env.LOGIN_CASE) return
    const x = await fixture(browser, options)
    try { await body(x); assert.deepEqual(x.state.errors, []); results.push({ name, status: 'PASS' }); console.log('PASS', name) }
    catch (error) { results.push({ name, status: 'FAIL', error: error.message }); console.error('FAIL', name, error.message); process.exitCode = 1 }
    finally { await x.context.close() }
  }
  try {
    for (const width of [1704, 390, 320]) {
      await run('terminal-layout-' + width, { width }, async ({ page }) => {
        await page.goto(base + '/login')
        await page.locator('.login-launch-brand h1').waitFor()
        assert.equal(await page.locator('.auth-page').count(), 0)
        await page.locator('canvas').waitFor()
        await page.getByRole('button', { name: '打开音乐播放器' }).waitFor()
        assert(await page.locator('.login-launch-footer a[href="/register"]').isVisible())
        assert(await page.locator('.login-launch-footer a[href="/"]').isVisible())
        if (width < 780) {
          const music = await page.locator('#MusicControl').boundingBox()
          const footer = await page.locator('.login-launch-footer').boundingBox()
          assert(music.y + music.height <= footer.y, 'Music control must not cover footer actions')
        }
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
        await page.screenshot({ path: path.join(output, `login-${width}.png`), fullPage: true })
        await openForm(page)
        const bounds = await page.getByRole('dialog').boundingBox()
        assert(bounds.x >= 0 && bounds.x + bounds.width <= width + 1)
        await page.screenshot({ path: path.join(output, `dialog-${width}.png`), fullPage: true })
        await page.locator('.login-access-close').click()
        await page.locator('.login-launch-footer a[href="/register"]').click()
        await page.locator('.register-compact-form').waitFor()
      })
    }
    await run('safe-redirects-and-csrf', {}, async ({ page, state }) => {
      for (const redirect of ['https://example.com', '//example.com', '/\\example.com', '/login?again=1', '/register', '/foods/../login']) {
        state.user = null
        await page.goto(base + '/login?redirect=' + encodeURIComponent(redirect))
        await openForm(page)
        await submit(page)
        await page.waitForURL(base + '/')
      }
      state.user = null
      await page.goto(base + '/login?redirect=' + encodeURIComponent('/about?from=login#story'))
      await openForm(page)
      await submit(page)
      await page.waitForURL(base + '/about?from=login#story')
      for (const call of state.calls.filter(call => call.path === '/api/auth/login')) assert.equal(call.headers['x-csrf-token'], 'login-fixture-csrf')
    })
    await run('failed-login-clears-password', {}, async ({ page, state }) => {
      state.loginStatus = 401
      await page.goto(base + '/login')
      await openForm(page)
      await submit(page)
      await page.locator('.login-form [role=alert]').waitFor()
      assert.equal(await page.locator('.login-form input[type=password]').inputValue(), '')
      assert.equal(new URL(page.url()).pathname, '/login')
    })
    await run('remembered-session-outage-and-retry', { remembered: true }, async ({ page, state }) => {
      await page.goto(base + '/login')
      await page.getByText(user.displayName, { exact: true }).waitFor()
      state.meStatus = 503
      await page.locator('.login-awaken-button').click()
      await page.getByRole('alert').waitFor()
      assert.equal(new URL(page.url()).pathname, '/login')
      state.meStatus = 200
      await page.locator('.login-awaken-button').click()
      await page.waitForURL(base + '/')
    })
    await run('account-switch-retries-server-logout', { remembered: true }, async ({ page, state }) => {
      await page.goto(base + '/login')
      await page.getByText(user.displayName, { exact: true }).waitFor()
      state.logoutStatus = 503
      await page.locator('.login-account-switch').click()
      await page.getByRole('alert').waitFor()
      assert.equal(await page.getByRole('dialog').count(), 0)
      state.logoutStatus = 200
      await page.locator('.login-account-switch').click()
      await page.getByRole('dialog').waitFor()
      assert.equal(state.calls.filter(call => call.path === '/api/auth/logout').length, 2)
      assert.equal(state.user, null)
      assert(state.calls.filter(call => call.path === '/api/auth/logout').every(call => call.headers['x-csrf-token'] === 'login-fixture-csrf'))
    })
    await run('revoked-remembered-session-opens-login', { remembered: true }, async ({ page, state }) => {
      await page.goto(base + '/login')
      await page.getByText(user.displayName, { exact: true }).waitFor()
      state.user = null
      await openForm(page)
      assert.equal(new URL(page.url()).pathname, '/login')
      assert.equal(state.calls.filter(call => call.path === '/api/auth/login').length, 0)
    })
    await run('cross-tab-change-rejects-stale-login', {}, async ({ page, state }) => {
      let release
      state.holdLogin = new Promise(resolve => { release = resolve })
      try {
        await page.goto(base + '/login')
        await openForm(page)
        const outgoing = page.waitForRequest('**/api/auth/login')
        await submit(page)
        await outgoing
        const verified = page.waitForResponse('**/api/auth/me')
        await page.evaluate(() => window.dispatchEvent(new StorageEvent('storage', {
          key: 'dayan-auth-session-change', newValue: 'other-tab-change',
        })))
        await verified
        release()
        await page.locator('.login-form [role=alert]').waitFor()
        assert.equal(new URL(page.url()).pathname, '/login')
        assert.equal(await page.locator('.login-form input[type=password]').inputValue(), '')
      } finally { release() }
    })
    await run('late-login-does-not-navigate-unmounted-page', {}, async ({ page, state }) => {
      let release
      state.holdLogin = new Promise(resolve => { release = resolve })
      try {
        await page.goto(base + '/login')
        await openForm(page)
        const outgoing = page.waitForRequest('**/api/auth/login')
        await submit(page)
        await outgoing
        await page.locator('#primary-navigation a[href="/about"]').click()
        await page.waitForURL(base + '/about')
        const response = page.waitForResponse('**/api/auth/login')
        release()
        await response
        await page.waitForTimeout(400)
        assert.equal(new URL(page.url()).pathname, '/about')
      } finally { release() }
    })
    await run('registration-return-and-theme', {}, async ({ page }) => {
      await page.goto(base + '/login?registered=1&username=new_member')
      await page.getByRole('dialog').waitFor()
      assert.equal(await page.locator('.login-form input[autocomplete=username]').inputValue(), 'new_member')
      assert(await page.locator('.form-success').isVisible())
      await page.locator('.login-access-close').click()
      await page.locator('.theme-toggle select').selectOption('dark')
      assert.equal(await page.locator('html').getAttribute('data-theme'), 'dark')
      await openForm(page)
      await page.waitForTimeout(250) // Let the dialog's 220 ms entrance transition finish for the visual capture.
      await page.screenshot({ path: path.join(output, 'login-dark.png'), fullPage: true })
    })
  } finally {
    await browser.close()
    fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify(results, null, 2))
  }
})().catch(error => { console.error(error); process.exitCode = 1 })
