import axios from 'axios'

export function apiErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError(error)) return fallback
  const status = error.response?.status
  const message = error.response?.data?.message
  // Only show bounded validation messages; never render HTML or server exception dumps.
  if ([400, 409, 422].includes(status ?? 0) && typeof message === 'string' && message.length <= 200) {
    return message
  }
  return status ? fallback + ' (HTTP ' + status + ')' : fallback
}
