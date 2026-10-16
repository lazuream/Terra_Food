<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { catalog, regions, demoMode } from '../api'
import type { Food, Region } from '../types'
import { t, toggleLanguage } from '../i18n'
import { errorText } from '../ui'
import FoodCard from '../components/FoodCard.vue'
const keyword = ref(''), selected = ref(0), page = ref(1), total = ref(0)
const foods = ref<Food[]>([]), places = ref<Region[]>([])
const busy = ref(false), error = ref('')
let generation = 0
async function load(more = false) {
  if (more && busy.value) return
  const request = ++generation
  const next = more ? page.value + 1 : 1
  busy.value = true; error.value = ''
  if (!more) { foods.value = []; total.value = 0 }
  try {
    const result = await catalog(keyword.value.trim(), selected.value, next)
    if (request !== generation) return
    foods.value = more ? [...foods.value, ...result.items] : result.items
    total.value = result.total; page.value = next
  } catch (cause) { if (request === generation) error.value = errorText(cause) }
  finally { if (request === generation) busy.value = false }
}
function select(id: number) { selected.value = id; void load() }
onMounted(async () => { void load(); try { places.value = await regions() } catch { /* Catalog error remains actionable. */ } })
</script>
<template>
  <view class="screen home-screen">
    <view class="brand-row"><text class="brand">{{ t('brand') }}</text><button class="language" @tap="toggleLanguage">{{ t('language') }}</button></view>
    <view v-if="demoMode" class="demo-banner">{{ t('demo') }}</view>
    <view class="hero">
      <text class="eyebrow">{{ t('eyebrow') }}</text>
      <text class="hero-title">{{ t('title') }}</text>
      <text class="hero-subtitle">{{ t('intro') }}</text>
      <view class="hero-seal"><text>寻</text><text>味</text></view>
      <view class="hero-line" />
    </view>
    <view class="search-box"><input v-model="keyword" class="search-input" :placeholder="t('search')" confirm-type="search" @confirm="load()" /><button class="search-button" @tap="load()">{{ t('searchAction') }}</button></view>
    <scroll-view scroll-x class="places"><view class="place-row"><button class="place" :class="{ selected: !selected }" @tap="select(0)">{{ t('all') }}</button><button v-for="place in places" :key="place.id" class="place" :class="{ selected: selected === place.id }" @tap="select(place.id)">{{ place.name }}</button></view></scroll-view>
    <view class="section-heading"><text class="section-title">{{ t('collection') }}</text><text class="caption">{{ String(total).padStart(2, '0') }}</text></view>
    <text class="section-note">{{ t('collectionNote') }}</text>
    <view v-if="error" class="state"><text>{{ error }}</text><button class="quiet-button" @tap="load()">{{ t('retry') }}</button></view>
    <view v-else-if="busy && !foods.length" class="state">{{ t('loading') }}</view>
    <view v-else-if="!foods.length" class="state"><text>{{ t('empty') }}</text><text class="muted">{{ t('emptyNote') }}</text></view>
    <view class="food-list"><FoodCard v-for="(item, index) in foods" :key="item.id" :food="item" :index="index" /></view>
    <button v-if="foods.length < total" class="quiet-button load-more" :disabled="busy" @tap="load(true)">{{ busy ? t('loading') : t('loadMore') }}</button>
    <text v-else-if="foods.length" class="end-note">{{ t('end') }}</text>
    <text class="footer-note">{{ t('about') }}</text>
  </view>
</template>
