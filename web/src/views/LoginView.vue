<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

import { isAdminRole, useAuth } from '../auth'
import { safeAuthRedirect } from '../authRedirect'
import { apiErrorMessage } from '../apiError'
import PasswordResetForm from '../components/PasswordResetForm.vue'
import LoginLoadingScene from '../components/LoginLoadingScene.vue'
import type { AuthUser, LoginPayload, LoginRole } from '../types'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuth()

const form = reactive<LoginPayload>({
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: '',
  role: route.query.role === 'ADMIN' ? 'ADMIN' : 'USER',
})
const loading = ref(false)
const checkingSession = ref(false)
const preloadProgress = ref(0)
const error = ref('')
const registrationSucceeded = route.query.registered === '1'
const resetSucceeded = ref(false)
const accessOpen = ref(registrationSucceeded || route.query.role === 'ADMIN')
const rememberedUser = computed(() => auth.currentUser.value)
// 登录成功后不再强制等待固定的终端动画时长；数据准备完成即可进入。
const PROGRESS_ESTIMATE_MS = 5000
const READY_HOLD_DURATION_MS = 200
let preloadStartedAt = 0
let progressTimer: ReturnType<typeof setInterval> | undefined
let sceneWaitTimer: ReturnType<typeof setTimeout> | undefined
let resolveSceneWait: ((active: boolean) => void) | undefined
let disposed = false
let logoutUnconfirmed = false

// Foreground and cross-tab verification is owned by main.ts and auth.ts.
// Clear sensitive input whenever that shared session generation changes.
watch(auth.sessionRevision, () => { form.password = '' })

function startPreloadProgress() {
  stopPreloadProgress()
  preloadStartedAt = performance.now()
  preloadProgress.value = 0
  progressTimer = setInterval(() => {
    // 展示进度不是后端百分比；请求未完成时最多到 94%，避免提前宣告就绪。
    const elapsed = performance.now() - preloadStartedAt
    preloadProgress.value = Math.min(94, (elapsed / PROGRESS_ESTIMATE_MS) * 94)
  }, 50)
}

function stopPreloadProgress() {
  if (progressTimer) clearInterval(progressTimer)
  progressTimer = undefined
}

function waitForScene(milliseconds: number): Promise<boolean> {
  return new Promise((resolve) => {
    resolveSceneWait = resolve
    sceneWaitTimer = setTimeout(() => {
      sceneWaitTimer = undefined
      resolveSceneWait = undefined
      resolve(true)
    }, milliseconds)
  })
}

onBeforeUnmount(() => {
  disposed = true
  form.password = ''
  stopPreloadProgress()
  if (sceneWaitTimer) clearTimeout(sceneWaitTimer)
  resolveSceneWait?.(false)
})

async function enterFor(user: Pick<AuthUser, 'id' | 'role'>) {
  if (disposed) return
  const revision = auth.getSessionRevision()
  stopPreloadProgress()
  preloadProgress.value = 100
  if (!(await waitForScene(READY_HOLD_DURATION_MS))) return
  // A logout, role change or account switch while the scene settles cancels navigation.
  if (disposed || revision !== auth.getSessionRevision() || !auth.sessionConfirmed.value
      || auth.currentUser.value?.id !== user.id) return
  await router.replace(isAdminRole(user.role) ? '/admin' : safeAuthRedirect(route.query.redirect, window.location.origin))
}

async function awaken() {
  if (loading.value || checkingSession.value) return
  checkingSession.value = true
  error.value = ''
  try {
    await auth.restoreSession(true)
  } finally {
    checkingSession.value = false
  }
  if (disposed) return
  if (!auth.sessionConfirmed.value) {
    error.value = t('login.sessionCheckFailed')
    return
  }
  const user = rememberedUser.value
  if (!user) {
    accessOpen.value = true
    return
  }

  loading.value = true
  startPreloadProgress()
  await nextTick()
  try {
    await enterFor(user)
  } finally {
    stopPreloadProgress()
    loading.value = false
  }
}

async function switchAccount() {
  if (loading.value || checkingSession.value) return
  checkingSession.value = true
  form.username = ''
  form.password = ''
  form.role = isAdminRole(rememberedUser.value?.role) ? 'ADMIN' : 'USER'
  error.value = ''
  resetSucceeded.value = false
  try {
    if (logoutUnconfirmed || auth.currentUser.value || !auth.sessionConfirmed.value) {
      logoutUnconfirmed = true
      await auth.logout()
      logoutUnconfirmed = false
    }
    if (!disposed) accessOpen.value = true
  } catch {
    if (!disposed) error.value = t('audit.sessionLogoutFailed')
  } finally { checkingSession.value = false }
}

function closeAccess() {
  form.password = ''
  error.value = ''
  accessOpen.value = false
}

function selectRole(role: LoginRole) {
  form.role = role
  error.value = ''
}

function handlePasswordReset(username: string) {
  form.username = username
  form.password = ''
  error.value = ''
  resetSucceeded.value = true
}

async function submit() {
  if (loading.value || checkingSession.value) return
  const payload = { ...form, username: form.username.trim(), password: form.password.normalize('NFKC').trim() }
  loading.value = true
  error.value = ''
  startPreloadProgress()
  // Render the scene without adding a fixed delay to authentication.
  await nextTick()

  try {
    if (disposed) return
    const user = await auth.login(payload)
    form.password = ''
    await enterFor(user)
  } catch (cause) {
    if (!disposed) error.value = apiErrorMessage(cause, t('login.error'))
  } finally {
    form.password = ''
    stopPreloadProgress()
    loading.value = false
  }
}
</script>

<template>
  <section class="login-launch-page" :class="{ 'is-loading': loading }" :aria-busy="loading">
    <!-- 保持同一个球体实例，从顶部待机位置连续移动到加载位置。 -->
    <LoginLoadingScene :active="loading" :progress="preloadProgress" />

    <div class="login-launch-brand" :inert="loading">
      <span class="login-launch-seal">炎</span>
      <p>{{ t('login.gatewayEyebrow') }}</p>
      <h1>{{ t('common.appName') }}</h1>
      <blockquote>{{ t('login.quote') }}</blockquote>

      <button class="login-awaken-button" type="button" :disabled="checkingSession" @click="awaken">
        <span>{{ t('login.awaken') }}</span>
      </button>

      <div class="login-launch-account">
        <button
          class="login-account-switch"
          type="button"
          :title="t('login.switchAccount')"
          :aria-label="t('login.switchAccount')"
          :disabled="checkingSession"
          @click="switchAccount"
        >{{ t('login.accountLabel') }}</button>
        <span aria-live="polite">{{ rememberedUser?.displayName || rememberedUser?.username || t('login.accountUnbound') }}</span>
      </div>
      <p v-if="error && !accessOpen" class="form-error" role="alert">{{ error }}</p>
    </div>

    <footer class="login-launch-footer" :inert="loading">
      <div class="login-launch-signature">
        <span>炎</span>
        <div>
          <b>{{ t('common.appName') }}</b>
          <small>{{ t('common.tagline') }}</small>
        </div>
      </div>
      <nav :aria-label="t('login.actions')">
        <RouterLink to="/register">{{ t('login.createAccount') }}</RouterLink>
        <RouterLink to="/">{{ t('login.guestBrowse') }}</RouterLink>
      </nav>
      <span class="login-launch-version">DAYAN TERMINAL · #1</span>
    </footer>

    <Teleport to="body">
    <Transition name="login-access">
      <div v-if="accessOpen && !loading" class="login-access-backdrop" @click.self="closeAccess">
        <div class="login-access-panel" role="dialog" aria-modal="true" :aria-label="t('login.title')" @keydown.esc="closeAccess">
          <button
            class="login-access-close"
            type="button"
            :aria-label="t('login.closeAccess')"
            @click="closeAccess"
          >×</button>

          <nav class="auth-mode-tabs" :aria-label="t('login.actions')"><RouterLink class="active" to="/login">{{ t('login.submit') }}</RouterLink><RouterLink to="/register">{{ t('login.registerNow') }}</RouterLink></nav>
          <div class="login-heading">
            <span class="login-access-seal">炎</span>
            <small>{{ t('login.welcome') }}</small>
            <h2>{{ t('login.title') }}</h2>
            <p>{{ t('login.description') }}</p>
          </div>

          <div class="role-tabs">
            <button type="button" :class="{ active: form.role === 'USER' }" @click="selectRole('USER')">
              <b>{{ t('login.user') }}</b>
              <span>{{ t('login.userHint') }}</span>
            </button>
            <button type="button" :class="{ active: form.role === 'ADMIN' }" @click="selectRole('ADMIN')">
              <b>{{ t('login.admin') }}</b>
              <span>{{ t('login.adminHint') }}</span>
            </button>
          </div>

          <form class="login-form" @submit.prevent="submit">
            <p v-if="registrationSucceeded" class="form-success">
              {{ t('login.registrationSuccess') }}
            </p>
            <p v-if="resetSucceeded" class="form-success">
              {{ t('login.resetSuccess') }}
            </p>
            <label>
              {{ t('login.username') }}
              <input v-model.trim="form.username" autocomplete="username" required autofocus>
            </label>
            <label>
              {{ t('login.password') }}
              <input v-model="form.password" type="password" autocomplete="current-password" required>
            </label>

            <p v-if="error" class="form-error" role="alert">{{ error }}</p>

            <button class="login-submit" :disabled="loading">
              {{ loading ? t('login.loggingIn') : t('login.submit') }}
            </button>
            <div class="register-entry">
              <span>{{ t('login.noAccount') }}</span>
              <RouterLink to="/register">{{ t('login.registerNow') }}</RouterLink>
            </div>
          </form>
          <PasswordResetForm :key="auth.sessionRevision.value" @reset="handlePasswordReset" />
        </div>
      </div>
    </Transition>
    </Teleport>
  </section>
</template>
