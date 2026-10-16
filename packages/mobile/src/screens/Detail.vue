<script setup lang="ts">
import { ref, watch } from 'vue'
import { food, favoriteStatus, setFavorite, imageUrl, demoMode, client } from '../api'
import { restoreSession, syncUser } from '../session'
import type { Food } from '../types'
import { t } from '../i18n'
import { errorText, openLogin } from '../ui'
const props = defineProps<{ foodId: number }>()
const item = ref<Food | null>(null), error = ref(''), busy = ref(false), saved = ref(false), saving = ref(false), broken = ref(false)
let generation = 0
async function load() {
  if (!props.foodId) return
  const epoch = ++generation; busy.value = true; error.value = ''; broken.value = false
  try {
    const result = await food(props.foodId)
    if (epoch !== generation) return
    item.value = result
    await restoreSession()
    if (!demoMode && client.user) saved.value = (await favoriteStatus(props.foodId)).favorited
  } catch (cause) { if (epoch === generation) error.value = errorText(cause) }
  finally { if (epoch === generation) busy.value = false }
}
async function save() {
  if (demoMode) return uni.showToast({ title: t('demoLogin'), icon: 'none' })
  if (!client.user) return openLogin()
  if (saving.value) return
  saving.value = true
  try { saved.value = (await setFavorite(props.foodId, !saved.value)).favorited }
  catch { syncUser(); uni.showToast({ title: t('saveFailed'), icon: 'none' }) }
  finally { saving.value = false }
}
watch(() => props.foodId, load, { immediate: true })
</script>
<template>
  <view class="screen detail-screen">
    <view v-if="demoMode" class="demo-banner">{{ t('demo') }}</view>
    <view v-if="busy" class="state">{{ t('loading') }}</view>
    <view v-else-if="error" class="state"><text>{{ error }}</text><button class="quiet-button" @tap="load">{{ t('retry') }}</button></view>
    <template v-else-if="item">
      <view class="detail-art tone-0"><image v-if="imageUrl(item.imageUrl) && !broken" class="food-image" :src="imageUrl(item.imageUrl)" mode="aspectFill" @error="broken = true" /><text v-else class="detail-glyph">{{ t('noImage') }}</text><text class="region-stamp">{{ item.region?.province }} · {{ item.region?.name }}</text></view>
      <text class="eyebrow detail-eyebrow">{{ t('detail') }} / {{ t('heat') }} {{ item.heat }}</text>
      <text class="detail-title">{{ item.name }}</text><text class="detail-summary">{{ item.summary }}</text>
      <view class="prose-section"><text class="section-title">{{ t('story') }}</text><text class="prose">{{ item.story || t('noStory') }}</text></view>
      <view class="prose-section"><text class="section-title">{{ t('ingredients') }}</text><text class="prose">{{ item.ingredients || '—' }}</text></view>
      <view v-if="item.address" class="prose-section"><text class="section-title">{{ t('address') }}</text><text class="prose">{{ item.address }}</text></view>
      <button class="primary-button" :disabled="saving" @tap="save">{{ saved ? t('saved') : t('save') }}</button>
    </template>
  </view>
</template>
