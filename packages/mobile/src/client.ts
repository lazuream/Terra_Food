import type { AuthUser } from './types.ts'

export type Method = 'GET' | 'POST' | 'DELETE'
export interface Reply { status: number; data: unknown; cookies?: string[] }
export type Transport = (path: string, method: Method, data: unknown, headers: Record<string, string>) => Promise<Reply>
export interface CookieStorage { read(): string; write(value: string): void }
export class ApiError extends Error {
  status: number
  code: string
  constructor(status: number, code: string, message: string) { super(message); this.status = status; this.code = code }
}

/** Native and mini program requests do not share the browser's cookie jar. Keep only our gateway's SESSION. */
export function sessionCookie(values: string[], previous: string): string {
  let result = previous
  for (const value of values) {
    const match = value.match(/(?:^|,\s*)SESSION=([^;,]*)/)
    if (match) result = /max-age=0(?:;|$)/i.test(value) || !match[1] ? '' : `SESSION=${match[1]}`
  }
  return result
}

export function createClient(transport: Transport, storage: CookieStorage) {
  let cookie = storage.read()
  let revision = 0
  let user: AuthUser | null = null
  let transition = false
  let csrf: Promise<{ token: string; headerName: string }> | undefined
  function clear() { revision++; user = null; cookie = ''; csrf = undefined; storage.write('') }
  async function send<T>(path: string, method: Method, data?: unknown, headers: Record<string, string> = {}): Promise<T> {
    const epoch = revision
    const reply = await transport(path, method, data, { ...headers, ...(cookie ? { Cookie: cookie } : {}) })
    if (epoch !== revision) throw new ApiError(409, 'SESSION_CHANGED', 'Session changed')
    const next = sessionCookie(reply.cookies || [], cookie)
    if (next !== cookie) { cookie = next; storage.write(next) }
    const payload = reply.data as { code?: string; message?: string } | null
    if (reply.status < 200 || reply.status >= 300) {
      if (reply.status === 403) csrf = undefined
      if (reply.status === 401 || payload?.code === 'IDENTITY_CHANGED') clear()
      throw new ApiError(reply.status, payload?.code || 'HTTP_ERROR', payload?.message || 'Request failed')
    }
    return reply.data as T
  }
  async function write<T>(path: string, data?: unknown, method: Method = 'POST', publicAuth = false): Promise<T> {
    if (!publicAuth && (transition || !user)) throw new ApiError(401, 'AUTHENTICATION_REQUIRED', 'Sign in first')
    const epoch = revision
    const identity = user?.id
    if (!csrf) csrf = send<{ token: string; headerName: string }>('/auth/csrf', 'GET').catch(error => { csrf = undefined; throw error })
    const token = await csrf
    if (revision !== epoch || user?.id !== identity) throw new ApiError(409, 'SESSION_CHANGED', 'Session changed')
    return send<T>(path, method, data, { [token.headerName]: token.token, ...(identity ? { 'X-Expected-User-Id': String(identity) } : {}) })
  }
  return {
    get user() { return user },
    get: <T>(path: string) => send<T>(path, 'GET'),
    write,
    async restore() { if (transition) return user; user = await send<AuthUser>('/auth/me', 'GET'); return user },
    async login(username: string, password: string) {
      if (transition) throw new ApiError(409, 'AUTH_BUSY', 'Please wait')
      transition = true; revision++; csrf = undefined; user = null
      try {
        user = await write<AuthUser>('/auth/login', { username, password, role: 'USER' }, 'POST', true)
        revision++; csrf = undefined // Login rotates both session and CSRF token.
        return user
      } finally { transition = false }
    },
    async logout() {
      if (transition) throw new ApiError(409, 'AUTH_BUSY', 'Please wait')
      // Clear local credentials only after the server confirms revocation.
      await write('/auth/logout')
      clear()
    },
  }
}
