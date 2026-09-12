import fs from 'node:fs'
import { chromium } from 'playwright'

const base = process.env.PERF_BASE_URL || 'http://web'
const output = process.env.PERF_RESULT || '/results/browser.json'
const profiles = [
  { name: 'mobile', viewport: { width: 390, height: 844 }, cpu: 4, latency: 150, down: 200_000, up: 93_750 },
  { name: 'desktop', viewport: { width: 1366, height: 768 }, cpu: 1, latency: 0, down: -1, up: -1 },
]
const browser = await chromium.launch({
  headless: true,
  ...(process.env.PERF_CHROME_PATH ? { executablePath: process.env.PERF_CHROME_PATH } : {}),
})
const results = []

for (const profile of profiles) {
  for (let run = 1; run <= 5; run++) {
    const context = await browser.newContext({ viewport: profile.viewport, serviceWorkers: 'block' })
    const page = await context.newPage()
    const session = await context.newCDPSession(page)
    await session.send('Emulation.setCPUThrottlingRate', { rate: profile.cpu })
    await session.send('Network.enable')
    if (profile.down > 0) await session.send('Network.emulateNetworkConditions', { offline: false, latency: profile.latency, downloadThroughput: profile.down, uploadThroughput: profile.up, connectionType: 'cellular3g' })
    const start = Date.now()
    let response
    try {
      response = await page.goto(base, { waitUntil: 'domcontentloaded', timeout: 30_000 })
      await page.locator('.explorer-search input').waitFor({ state: 'visible', timeout: 30_000 })
      const searchMs = Date.now() - start
      await page.locator('.explorer-card').first().waitFor({ state: 'visible', timeout: 30_000 })
      const catalogMs = Date.now() - start
      const marks = await page.evaluate(() => performance.getEntriesByType('mark').map((entry) => ({ name: entry.name, startTime: entry.startTime })))
      results.push({ profile: profile.name, run, status: response?.status(), searchMs, catalogMs, marks })
    } catch (error) {
      results.push({ profile: profile.name, run, error: String(error), elapsedMs: Date.now() - start })
    }
    await context.close()
  }
}

{
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, serviceWorkers: 'block' })
  const page = await context.newPage()
  const requested = []
  page.on('request', (request) => requested.push(request.url()))
  await page.route('**/api/auth/me', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 5_000))
    await route.continue()
  })
  await page.route('**/api/food-tags**', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 5_000))
    await route.continue()
  })
  await page.route(/(?:tile\.openstreetmap|tianditu|autonavi|amap)/i, (route) => route.abort())
  const start = Date.now()
  try {
    await page.goto(base, { waitUntil: 'domcontentloaded', timeout: 30_000 })
    await page.locator('.explorer-card').first().waitFor({ state: 'visible', timeout: 3_000 })
    results.push({
      profile: 'dependency-fault', run: 1, catalogMs: Date.now() - start,
      privateRequestsBeforeIdentity: requested.filter((url) => /favorites|wishlist|achievements/.test(url)).length,
      audioRequests: requested.filter((url) => /\.(mp3|ogg|wav)(?:\?|$)/i.test(url)).length,
    })
  } catch (error) {
    results.push({ profile: 'dependency-fault', run: 1, error: String(error), elapsedMs: Date.now() - start })
  }
  await context.close()
}
await browser.close()
fs.mkdirSync(new URL('.', `file://${output}`).pathname, { recursive: true })
fs.writeFileSync(output, JSON.stringify({ generatedAt: new Date().toISOString(), base, results }, null, 2))
console.table(results.map(({ profile, run, searchMs, catalogMs, error }) => ({ profile, run, searchMs, catalogMs, error })))
if (results.some((row) => row.error)) process.exitCode = 1
