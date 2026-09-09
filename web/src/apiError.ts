import axios from 'axios'

export function apiErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError(error)) return fallback
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') return fallback + '（请求超时，请重试）'
  const status = error.response?.status
  const message = error.response?.data?.message
  const requestId = error.response?.data?.requestId
  // Only show bounded validation messages; never render HTML or server exception dumps.
  if ([400, 409, 413, 422].includes(status ?? 0) && typeof message === 'string' && message.length <= 200) {
    return message + (typeof requestId === 'string' ? `（问题编号：${requestId}）` : '')
  }
  return status ? fallback + ' (HTTP ' + status + ')' : fallback
}
