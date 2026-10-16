<script setup lang="ts">
import { ref } from 'vue'
import { login, demoMode } from '../api'
import { restoreSession, syncUser } from '../session'
import { ApiError } from '../client'
import { errorText } from '../ui'
import { t, toggleLanguage } from '../i18n'
const username = ref(''), password = ref(''), busy = ref(false), error = ref('')
async function submit() {
  if (busy.value) return
  if (demoMode) { error.value = t('demoLogin'); return }
  if (!username.value.trim() || !password.value) { error.value = t('required'); return }
  busy.value = true; error.value = ''
  try {
    await restoreSession()
    await login(username.value.trim(), password.value)
    syncUser(); password.value = ''
    uni.switchTab({ url: '/pages/profile/index' })
  } catch (cause) { error.value = cause instanceof ApiError && cause.status === 401 ? t('loginFailed') : errorText(cause) }
  finally { busy.value = false }
}
</script>
<template>
  <view class="screen login-screen">
    <view class="brand-row"><text class="brand">{{ t('brand') }}</text><button class="language" @tap="toggleLanguage">{{ t('language') }}</button></view>
    <view class="login-seal">食</view><text class="hero-title">{{ t('loginTitle') }}</text><text class="hero-subtitle">{{ t('loginIntro') }}</text>
    <view class="login-form">
      <text class="field-label">{{ t('username') }}</text><input v-model="username" class="form-input" :placeholder="t('username')" :disabled="busy" maxlength="160" />
      <text class="field-label">{{ t('password') }}</text><input v-model="password" class="form-input" password :placeholder="t('password')" :disabled="busy" maxlength="256" confirm-type="done" @confirm="submit" />
      <text v-if="error" class="error-message">{{ error }}</text><button class="primary-button" :disabled="busy" @tap="submit">{{ busy ? t('loginBusy') : t('loginAction') }}</button>
    </view>
    <text class="section-note">{{ t('loginNote') }}</text>
  </view>
</template>
