<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { client, favorites, demoMode } from '../api'
import { currentUser, restoreSession, sessionError, syncUser } from '../session'
import type { Food } from '../types'
import { t, toggleLanguage } from '../i18n'
import { errorText, openLogin } from '../ui'
import FoodCard from '../components/FoodCard.vue'
const items = ref<Food[]>([]), busy = ref(false), error = ref('')
async function load() {
  busy.value = true; error.value = ''; items.value = []
  await restoreSession()
  try { if (currentUser.value) items.value = (await favorites()).items }
  catch (cause) { syncUser(); error.value = errorText(cause) }
  finally { busy.value = false }
}
async function logout() {
  if (busy.value) return
  busy.value = true
  try { await client.logout(); syncUser(); items.value = [] }
  catch { uni.showToast({ title: t('logoutFailed'), icon: 'none' }) }
  finally { busy.value = false }
}
onShow(load)
</script>
<template>
  <view class="screen profile-screen">
    <view class="brand-row"><text class="brand">{{ t('profileTitle') }}</text><button class="language" @tap="toggleLanguage">{{ t('language') }}</button></view>
    <view v-if="demoMode" class="demo-banner">{{ t('demo') }}</view>
    <view v-if="!currentUser" class="guest-card"><text class="hero-title">{{ t('guest') }}</text><text class="hero-subtitle">{{ t('guestNote') }}</text><button class="primary-button" @tap="openLogin">{{ t('login') }}</button></view>
    <template v-else><view class="profile-header"><view class="avatar">{{ currentUser.displayName.slice(0, 1) }}</view><view><text class="food-name">{{ currentUser.displayName }}</text><text class="caption">@{{ currentUser.username }}</text></view></view><text class="section-title">{{ t('favorites') }}</text></template>
    <view v-if="sessionError || error" class="state"><text>{{ error || t('sessionError') }}</text><button class="quiet-button" @tap="load">{{ t('retry') }}</button></view>
    <view v-else-if="busy" class="state">{{ t('loading') }}</view>
    <view v-else-if="currentUser && !items.length" class="state">{{ t('favoritesEmpty') }}</view>
    <view class="food-list"><FoodCard v-for="(item, index) in items" :key="item.id" :food="item" :index="index" /></view>
    <button v-if="currentUser" class="quiet-button" :disabled="busy" @tap="logout">{{ t('logout') }}</button>
    <text class="footer-note">{{ t('about') }}</text>
  </view>
</template>
