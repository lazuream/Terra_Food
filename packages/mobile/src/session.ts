import { ref } from 'vue'
import { client, demoMode } from './api'
import { ApiError } from './client'
import type { AuthUser } from './types'

export const currentUser = ref<AuthUser | null>(null)
export const sessionError = ref(false)
let restoring: Promise<void> | undefined
export function restoreSession(): Promise<void> {
  if (demoMode) return Promise.resolve()
  if (!restoring) restoring = (async () => {
    try { currentUser.value = await client.restore(); sessionError.value = false }
    catch (error) {
      currentUser.value = client.user
      sessionError.value = !(error instanceof ApiError && (error.status === 401 || error.code === 'SESSION_CHANGED'))
    } finally { restoring = undefined }
  })()
  return restoring
}
export function syncUser() { currentUser.value = client.user }
