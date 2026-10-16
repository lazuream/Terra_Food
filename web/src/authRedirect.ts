/** Authentication may only return to a local, non-authentication route. */
export function safeAuthRedirect(value: unknown, origin: string): string {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || /[\\\x00-\x1f]/.test(value)) return '/'
  try {
    const destination = new URL(value, origin)
    if (destination.origin !== origin || /^\/(login|register)(\/|$)/.test(destination.pathname)) return '/'
    return destination.pathname + destination.search + destination.hash
  } catch { return '/' }
}
