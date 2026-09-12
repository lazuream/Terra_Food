import { readonly, ref } from 'vue'
import { isAxiosError } from 'axios'

import { getCurrentUser, login as requestLogin, logout as requestLogout } from './api'
import { useAchievementNotifications } from './achievement'
import type { AuthUser, LoginPayload, UserRole } from './types'
import { clearDraftsForUser } from './drafts'

const currentUser = ref<AuthUser | null>(null)
const achievementNotifications = useAchievementNotifications()
let sessionChecked = false
let lastRestoreAttemptAt = 0
let sessionRevision = 0
let restoring: { revision: number; promise: Promise<void> } | undefined
export const AUTH_SESSION_CHANGE_KEY = 'dayan-auth-session-change'

function notifySessionChange() {
  try {
    // 只广播变更信号；身份凭证始终由 HttpOnly Cookie 管理。
    localStorage.setItem(AUTH_SESSION_CHANGE_KEY, `${Date.now()}-${Math.random()}`)
  } catch {
    // 禁用浏览器存储时仍可通过切回页面刷新会话。
  }
}

function applyUser(user: AuthUser | null) {
  if (currentUser.value?.username !== user?.username) achievementNotifications.clear()
  currentUser.value = user
}
// 恢复失败后短时冷却，避免弱网下每次导航都重复打 /auth/me；
// 冷却期过后仍会重试（不把“单次失败”升级成永久登出）。
const RESTORE_COOLDOWN_MS = 30_000

export function isAdminRole(role?: UserRole): boolean {
  return role === 'ADMIN' || role === 'SUB_ADMIN'
}

export function useAuth() {
  function getSessionRevision() { return sessionRevision }

  async function restoreSession(force = false) {
    if (restoring?.revision === sessionRevision) return restoring.promise
    if (!force && sessionChecked) {
      return
    }

    const now = Date.now()
    if (!force && now - lastRestoreAttemptAt < RESTORE_COOLDOWN_MS) {
      return
    }
    lastRestoreAttemptAt = now

    const revision = sessionRevision
    const promise = (async () => {
      try {
        const user = await getCurrentUser()
        if (revision !== sessionRevision) return
        applyUser(user)
        sessionChecked = true
        void achievementNotifications.load()
      } catch (error) {
        if (revision !== sessionRevision) return
        if (isAxiosError(error) && error.response?.status === 401) {
          applyUser(null)
          sessionChecked = true
        } else {
          // 网络故障不等于登出，保留当前身份并允许后续重试。
          sessionChecked = false
        }
      } finally {
        if (restoring?.revision === revision) restoring = undefined
      }
    })()
    restoring = { revision, promise }
    return promise
  }

  async function login(payload: LoginPayload) {
    const revision = ++sessionRevision
    const user = await requestLogin(payload)
    if (revision !== sessionRevision) throw new Error('登录状态已在其他页面发生变化')
    applyUser(user)
    sessionChecked = true
    notifySessionChange()
    void achievementNotifications.load()
    return user
  }

  async function logout() {
    const exitingUserId = currentUser.value?.id
    ++sessionRevision
    try {
      await requestLogout()
    } finally {
      if (exitingUserId != null) clearDraftsForUser(exitingUserId)
      clearSession(true)
    }
  }

  function clearSession(broadcast = true) {
    ++sessionRevision
    currentUser.value = null
    achievementNotifications.clear()
    sessionChecked = true
    if (broadcast) notifySessionChange()
  }

  function setCurrentUser(user: AuthUser, expectedRevision = sessionRevision, expectedUserId = currentUser.value?.id) {
    if (expectedRevision !== sessionRevision || expectedUserId !== currentUser.value?.id || user.id !== expectedUserId) return false
    applyUser(user)
    return true
  }

  async function handleExternalSessionChange() {
    clearSession(false)
    sessionChecked = false
    await restoreSession(true)
  }

  return {
    currentUser: readonly(currentUser),
    restoreSession,
    login,
    logout,
    clearSession,
    setCurrentUser,
    getSessionRevision,
    handleExternalSessionChange,
  }
}
