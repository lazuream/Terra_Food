import fs from 'node:fs'
import { performance } from 'node:perf_hooks'

const base = process.env.PERF_BASE_URL || 'http://web'
const output = process.env.PERF_RESULT || '/results/api.json'
const optimizedPaths = [
  ['/api/foods/catalog?page=1&pageSize=20&compact=true', 500],
  ['/api/foods/catalog?keyword=%E7%89%9B%E8%82%89&page=1&pageSize=20&compact=true', 800],
  ['/api/foods/map-clusters?minLatitude=18&maxLatitude=54&minLongitude=73&maxLongitude=136&zoom=4', 800],
]
const baselinePaths = [
  ['/api/foods/catalog?page=1&pageSize=20', 500],
  ['/api/foods/catalog?keyword=%E7%89%9B%E8%82%89&page=1&pageSize=20', 800],
  ['/api/foods/markers?minLatitude=18&maxLatitude=54&minLongitude=73&maxLongitude=136', 800],
]
const paths = process.env.PERF_API_MODE === 'baseline' ? baselinePaths : optimizedPaths
const concurrencies = [1, 5, 10, 20]
const results = []

for (const [path, target] of paths) {
  for (const concurrency of concurrencies) {
    const samples = []
    let failures = 0
    await Promise.all(Array.from({ length: concurrency * 5 }, async () => {
      const start = performance.now()
      try {
        const response = await fetch(base + path)
        if (!response.ok) failures++
        await response.arrayBuffer()
      } catch { failures++ }
      samples.push(performance.now() - start)
    }))
    samples.sort((a, b) => a - b)
    const percentile = (p) => samples[Math.min(samples.length - 1, Math.ceil(samples.length * p) - 1)]
    results.push({ path, concurrency, count: samples.length, failures, medianMs: percentile(.5), p95Ms: percentile(.95), maxMs: samples.at(-1), targetMs: target })
    if (failures || percentile(.95) > target * 4) break
  }
}

fs.mkdirSync(new URL('.', `file://${output}`).pathname, { recursive: true })
fs.writeFileSync(output, JSON.stringify({ generatedAt: new Date().toISOString(), base, results }, null, 2))
console.table(results)
if (results.some((row) => row.failures)) process.exitCode = 1
