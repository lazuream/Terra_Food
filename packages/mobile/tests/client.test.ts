import test from 'node:test'
import assert from 'node:assert/strict'
import { createClient, sessionCookie } from '../src/client.ts'
import type { Reply, Transport } from '../src/client.ts'
const user = { id: 7, username: 'test', displayName: 'Test' }
const memory = () => { let value = ''; return { read: () => value, write: (next: string) => { value = next } } }

test('cookie adapter retains only SESSION and honors expiry', () => {
  assert.equal(sessionCookie(['SESSION=abc==; Path=/; HttpOnly', 'tracking=no'], ''), 'SESSION=abc==')
  assert.equal(sessionCookie(['SESSION=; Max-Age=0; Path=/'], 'SESSION=old'), '')
  assert.equal(sessionCookie(['other=secret'], 'SESSION=old'), 'SESSION=old')
})
test('login rotates cookie, obtains fresh CSRF and pins identity on subsequent writes', async () => {
  const calls: Array<{ path: string; headers: Record<string, string> }> = []
  let tokens = 0
  const client = createClient(async (path, _method, _data, headers) => {
    calls.push({ path, headers })
    if (path === '/auth/csrf') return { status: 200, data: { token: 'token' + ++tokens, headerName: 'X-CSRF-TOKEN' }, cookies: tokens === 1 ? ['SESSION=guest; Path=/'] : [] }
    if (path === '/auth/login') return { status: 200, data: user, cookies: ['SESSION=signed; HttpOnly'] }
    return { status: 200, data: { favorited: true } }
  }, memory())
  await client.login('test', 'password')
  await client.write('/profile/favorites/1')
  assert.equal(calls[1]!.headers.Cookie, 'SESSION=guest')
  assert.equal(calls[1]!.headers['X-CSRF-TOKEN'], 'token1')
  assert.deepEqual(calls[3]!.headers, { Cookie: 'SESSION=signed', 'X-CSRF-TOKEN': 'token2', 'X-Expected-User-Id': '7' })
})
test('a delayed response from the old session cannot replace the newly signed-in session', async () => {
  let resolve!: (reply: Reply) => void
  const store = memory()
  const transport: Transport = async path => {
    if (path === '/slow') return new Promise(done => { resolve = done })
    if (path === '/auth/csrf') return { status: 200, data: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } }
    return { status: 200, data: user, cookies: ['SESSION=new'] }
  }
  const client = createClient(transport, store)
  const old = client.get('/slow')
  const rejected = assert.rejects(old, { code: 'SESSION_CHANGED' })
  await client.login('test', 'password')
  resolve({ status: 200, data: {}, cookies: ['SESSION=old'] })
  await rejected
  assert.equal(store.read(), 'SESSION=new')
})
test('private writes require confirmed identity and revocation clears credentials', async () => {
  const store = memory()
  const client = createClient(async path => path === '/auth/me' ? { status: 200, data: user }
    : path === '/auth/csrf' ? { status: 200, data: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } }
    : { status: 401, data: { code: 'SESSION_REVOKED' } }, store)
  await assert.rejects(client.write('/profile/favorites/1'), { code: 'AUTHENTICATION_REQUIRED' })
  await client.restore()
  await assert.rejects(client.write('/profile/favorites/1'), { code: 'SESSION_REVOKED' })
  assert.equal(client.user, null)
  assert.equal(store.read(), '')
})
test('network failure on logout does not falsely report server-side revocation', async () => {
  const client = createClient(async path => path === '/auth/me' ? { status: 200, data: user }
    : path === '/auth/csrf' ? { status: 200, data: { token: 'csrf', headerName: 'X-CSRF-TOKEN' } }
    : Promise.reject(new Error('offline')), memory())
  await client.restore()
  await assert.rejects(client.logout(), /offline/)
  assert.equal(client.user?.id, 7)
})
