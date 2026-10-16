<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Food } from '../types'
import { imageUrl } from '../api'
import { t } from '../i18n'
import { openFood } from '../ui'
const props = defineProps<{ food: Food; index?: number }>()
const broken = ref(false)
watch(() => props.food.imageUrl, () => { broken.value = false })
</script>
<template>
  <view class="food-card" @tap="openFood(food.id)">
    <view class="food-art" :class="'tone-' + ((index || 0) % 3)">
      <image v-if="imageUrl(food.imageVariants?.small || food.imageUrl) && !broken" class="food-image" :src="imageUrl(food.imageVariants?.small || food.imageUrl)" mode="aspectFill" @error="broken = true" />
      <text v-else class="food-glyph">{{ t('noImage') }}</text>
      <text class="region-stamp">{{ food.region?.province || '—' }}</text>
    </view>
    <view class="food-copy">
      <text class="caption">{{ food.region?.name }} · {{ String((index || 0) + 1).padStart(2, '0') }}</text>
      <text class="food-name">{{ food.name }}</text>
      <text class="food-summary">{{ food.summary }}</text>
      <view class="card-bottom"><text>{{ t('read') }}</text><text>↗</text></view>
    </view>
  </view>
</template>
