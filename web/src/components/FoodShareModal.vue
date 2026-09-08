<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
import axios from 'axios'
import { toBlob } from 'html-to-image'
import { getAchievements, getMyEtchings, getMyProfileStats, getUserPublic } from '../api'
import { useAuth } from '../auth'
import type { Achievement, EtchingDesign, Food, ProfileStats, UserPublic } from '../types'
import HexEtching from './HexEtching.vue'

const props = defineProps<{ food: Food }>()
const emit = defineEmits<{ close: [] }>()
const { t, locale } = useI18n()
const router = useRouter()
const user = useAuth().currentUser
const dialog = ref<HTMLDialogElement>()
const viewport = ref<HTMLElement>()
const card = ref<HTMLElement>()
const scale = ref(1)
const loading = ref(true)
const exporting = ref(false)
const importing = ref(false)
const error = ref('')
const loadFailed = ref(false)
const profileWarning = ref(false)
const sealLoadFailed = ref(false)
const imageWarning = ref(false)
const stats = ref<ProfileStats>()
const statsLoading = ref(false)
const statsError = ref('')
const profile = ref<UserPublic>()
const achievements = ref<Achievement[]>([])
const etchings = ref<EtchingDesign[]>([])
const foodImage = ref('')
const avatar = ref('')
const background = ref('')
const defaultBackground = ref('')
const qr = ref('')
const preview = ref('')
let observer: ResizeObserver | undefined
let disposed = false
let uploadVersion = 0
const controller = new AbortController()
const openedAt = new Date()
const shareUrl = new URL(router.resolve('/foods/' + props.food.id).href, window.location.origin).href
const localAddress = ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname)
const displayName = computed(() => profile.value?.displayName || user.value?.displayName || t('share.guest'))
const date = computed(() => openedAt.toLocaleDateString(locale.value))
const seals = computed(() => [
  ...etchings.value.map(item => ({ kind: 'custom' as const, ...item })),
  ...achievements.value.map(item => ({ kind: 'achievement' as const, ...item })),
].sort((a, b) => Number(b.selected) - Number(a.selected)))
const visibleSeals = computed(() => seals.value.slice(0, 1))

// Keep every encoded module and a four-module quiet zone intact.
// Finder ornaments stay within their original 7x7 footprints.
function createShareQr(url: string): string {
  const { modules } = QRCode.create(url, { errorCorrectionLevel: 'H' })
  const unit = 10
  const margin = 4
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = (modules.size + margin * 2) * unit
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Canvas unavailable')
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  const finders = [[0, 0], [modules.size - 7, 0], [0, modules.size - 7]]
  context.fillStyle = '#30231c'
  for (let row = 0; row < modules.size; row++) {
    for (let column = 0; column < modules.size; column++) {
      if (!modules.get(row, column) || finders.some(([x, y]) =>
        column >= x! && column < x! + 7 && row >= y! && row < y! + 7)) continue
      context.beginPath()
      context.roundRect((column + margin) * unit, (row + margin) * unit, unit, unit, 2)
      context.fill()
    }
  }
  for (const [column, row] of finders) {
    const x = (column! + margin) * unit
    const y = (row! + margin) * unit
    for (const [inset, width, color, radius] of [
      [0, 7, '#842d26', 12], [1, 5, '#ffffff', 6], [2, 3, '#842d26', 5],
    ] as const) {
      context.fillStyle = color
      context.beginPath()
      context.roundRect(x + inset * unit, y + inset * unit, width * unit, width * unit, radius)
      context.fill()
    }
  }
  return canvas.toDataURL('image/png')
}

function readBlob(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(new Error('image read failed'))
    reader.readAsDataURL(blob)
  })
}
async function decodeImage(src: string) {
  const img = new Image()
  img.src = src
  await img.decode()
  if (img.naturalWidth * img.naturalHeight > 24_000_000) throw new Error('image too large')
}
// Embed assets before export, so missing/CORS-blocked images get an explicit fallback.
async function embedImage(url?: string | null, optional = false): Promise<string> {
  if (!url) return ''
  try {
    const response = await fetch(url, { signal: AbortSignal.any([controller.signal, AbortSignal.timeout(10_000)]) })
    if (!response.ok) throw new Error('image unavailable')
    const blob = await response.blob()
    if (blob.size > 10 * 1024 * 1024) throw new Error('image too large')
    const data = await readBlob(blob)
    await decodeImage(data)
    return data
  } catch {
    if (!optional && !disposed) imageWarning.value = true
    return ''
  }
}
async function loadStats() {
  if (!user.value || statsLoading.value || disposed) return
  statsLoading.value = true
  statsError.value = ''
  try {
    const counts = await getMyProfileStats()
    if (!counts || !Number.isSafeInteger(counts.viewedFoodCount) || counts.viewedFoodCount < 0
      || !Number.isSafeInteger(counts.favoriteCount) || counts.favoriteCount < 0) {
      throw new Error('invalid-stats')
    }
    if (!disposed) stats.value = counts
  } catch (cause) {
    if (disposed) return
    const status = axios.isAxiosError(cause) ? cause.response?.status : undefined
    statsError.value = status
      ? t('share.statsHttpError', { status })
      : t(axios.isAxiosError(cause) ? 'share.statsNetworkError' : 'share.statsFormatError')
  } finally {
    if (!disposed) statsLoading.value = false
  }
}

async function load() {
  loading.value = true
  loadFailed.value = false
  profileWarning.value = false
  sealLoadFailed.value = false
  clearPreview()
  error.value = ''
  imageWarning.value = false
  // Start statistics immediately; image downloads must not delay the counts.
  const statsTask = loadStats()
  try {
    const [qrData, dishImage, art] = await Promise.all([
      createShareQr(shareUrl),
      embedImage(props.food.imageUrl),
      embedImage(import.meta.env.BASE_URL + 'share-backgrounds/default.jpg', true),
    ])
    qr.value = qrData
    foodImage.value = dishImage
    if (background.value === defaultBackground.value) background.value = art
    defaultBackground.value = art
    if (user.value) {
      // Optional profile endpoints may be unavailable during rolling deployments.
      // Keep each successful section and never gate PNG export on these requests.
      const results = await Promise.allSettled([
        statsTask,
        (async () => {
          const publicUser = await getUserPublic(user.value!.id)
          if (!publicUser || typeof publicUser.displayName !== 'string') throw new Error('Invalid public profile')
          profile.value = publicUser
          avatar.value = await embedImage(publicUser.avatarUrl)
        })(),
        (async () => {
          const badges = await getAchievements()
          if (!Array.isArray(badges)) throw new Error('Invalid achievements')
          achievements.value = await Promise.all(badges.map(async badge => ({
            ...badge, imageUrl: await embedImage(badge.imageUrl),
          })))
        })(),
        (async () => {
          const designs = await getMyEtchings()
          if (!Array.isArray(designs)) throw new Error('Invalid etchings')
          etchings.value = designs
        })(),
      ])
      if (!disposed) {
        profileWarning.value = results.some(result => result.status === 'rejected')
        sealLoadFailed.value = results[2].status === 'rejected' || results[3].status === 'rejected'
      }
      if (!avatar.value && !profile.value) avatar.value = await embedImage(user.value?.avatarUrl)
    }
  } catch {
    if (!disposed) {
      loadFailed.value = true
      error.value = t('share.loadError')
    }
  } finally {
    await statsTask
    if (!disposed) loading.value = false
  }
}
function clearPreview() {
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = ''
}
async function importBackground(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 10 * 1024 * 1024) {
    error.value = t('share.fileError')
    return
  }
  const version = ++uploadVersion
  importing.value = true
  error.value = ''
  try {
    const data = await readBlob(file)
    await decodeImage(data)
    if (disposed || version !== uploadVersion) return
    background.value = data
    clearPreview()
  } catch {
    if (!disposed) error.value = t('share.fileError')
  } finally {
    if (!disposed && version === uploadVersion) importing.value = false
  }
}
function resetBackground() {
  background.value = defaultBackground.value
  error.value = ''
  clearPreview()
}
async function exportCard() {
  if (!card.value || loading.value || exporting.value || importing.value) return
  exporting.value = true
  error.value = ''
  try {
    await document.fonts.ready
    await nextTick()
    await Promise.all(Array.from(card.value.querySelectorAll('img')).map(img => img.decode()))
    const blob = await toBlob(card.value, { width: 1200, height: 840, pixelRatio: 2, skipFonts: true })
    if (!blob) throw new Error('empty export')
    if (disposed) return
    clearPreview()
    preview.value = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = preview.value
    link.download = (props.food.name.replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_').slice(0, 70) || 'food') + '-' + props.food.id + '.png'
    document.body.append(link)
    link.click()
    link.remove()
  } catch {
    if (!disposed) error.value = t('share.exportError')
  } finally {
    if (!disposed) exporting.value = false
  }
}
watch(() => user.value?.id, () => emit('close'))

onMounted(() => {
  dialog.value?.showModal()
  observer = new ResizeObserver(entries => {
    const width = entries[0]?.contentRect.width
    if (width) scale.value = width / 1200
  })
  if (viewport.value) observer.observe(viewport.value)
  void load()
})
onBeforeUnmount(() => {
  disposed = true
  controller.abort()
  observer?.disconnect()
  clearPreview()
})
</script>

<template>
  <Teleport to="body">
    <dialog ref="dialog" class="food-share-dialog" aria-labelledby="share-title" @close="emit('close')" @click="event => { if (event.target === dialog) dialog?.close() }">
      <header class="share-toolbar">
        <div><small>{{ t('share.eyebrow') }}</small><h2 id="share-title">{{ t('share.title') }}</h2></div>
        <button type="button" :aria-label="t('share.close')" autofocus @click="dialog?.close()">×</button>
      </header>
      <p class="share-intro">{{ t('share.intro') }}</p>
      <p v-if="loading" class="share-notice" role="status">{{ t('share.loading') }}</p>
      <p v-if="statsError" class="share-notice share-error share-stats-error" role="status">
        {{ statsError }}
        <button type="button" :disabled="statsLoading || exporting" @click="loadStats">{{ t('share.retryStats') }}</button>
      </p>
      <div ref="viewport"  class="share-viewport" :aria-busy="loading">
        <div class="share-scale" :style="{ transform: 'scale(' + scale + ')' }">
          <article ref="card" class="share-card">
            <img v-if="background" class="share-art" :src="background" alt="">
            <div class="share-shade" />
            <section class="share-dish">
              <div class="share-kicker"><span>{{ t('share.archive') }}</span><span>No. {{ String(food.id).padStart(5, '0') }}</span></div>
              <div class="share-dish-image"><img v-if="foodImage" :src="foodImage" :alt="food.name"><span v-else>{{ t('share.noPhoto') }}</span></div>
              <small class="share-region">{{ food.region.province }} / {{ food.region.name }}</small>
              <h2>{{ food.name }}</h2>
              <p class="share-summary">{{ food.summary }}</p>
              <div class="share-ingredients"><small>{{ t('detail.ingredients') }}</small><p>{{ food.ingredients }}</p></div>
              <p class="share-credit">{{ t('detail.uploadedBy') }} · {{ food.creator.displayName }}</p>
            </section>
            <section class="share-person">
              <div class="share-identity share-panel">
                <div class="share-avatar"><img v-if="avatar" :src="avatar" alt=""><span v-else>{{ Array.from(displayName)[0] }}</span></div>
                <div><small>{{ t('share.sharedBy') }}</small><h3>{{ displayName }}</h3></div>
              </div>
              <p class="share-signature" :title="profile?.signature || user?.signature || t('share.signature')">{{ profile?.signature || user?.signature || t('share.signature') }}</p>
              <div class="share-stats share-panel">
                <div><small>{{ t('share.views') }}</small><strong>{{ stats?.viewedFoodCount.toLocaleString(locale) ?? (statsLoading ? '…' : '—') }}</strong></div>
                <div><small>{{ t('share.favorites') }}</small><strong>{{ stats?.favoriteCount.toLocaleString(locale) ?? (statsLoading ? '…' : '—') }}</strong></div>
              </div>
              <div class="share-seals share-panel">
                <div class="share-seals-heading"><small>{{ t('share.seals') }}</small></div>
                <div v-if="visibleSeals.length" class="share-seal-grid">
                  <div v-for="seal in visibleSeals" :key="seal.kind + '-' + seal.id" class="share-seal" :class="{ selected: seal.selected }">
                    <HexEtching v-if="seal.kind === 'custom'" :layer-one="seal.layerOne" />
                    <img v-else-if="seal.imageUrl" :src="seal.imageUrl" alt="">
                    <span v-else class="share-seal-fallback">◇</span>
                    <small>{{ seal.name }}</small>
                  </div>
                </div>
                <p v-else class="share-empty">{{ sealLoadFailed ? t('profile.sealLoadError') : user ? t('share.noSeals') : t('share.guestHint') }}</p>
              </div>
            </section>
            <aside class="share-rail">
              <div><strong>{{ t('common.appName') }}</strong><small>{{ t('share.edition') }}</small><time>{{ date }}</time></div>
              <div class="share-qr"><img v-if="qr" :src="qr" :alt="t('share.qrAlt')"><p>{{ t('share.scan') }}</p><small>{{ t('share.scanHint') }}</small></div>
              <span class="share-rail-bottom">{{ t('share.footer') }}</span>
            </aside>
          </article>
        </div>
      </div>
      <p v-if="error" class="share-notice share-error" role="alert">{{ error }} <button v-if="loadFailed && !loading" type="button" @click="load">{{ t('share.retry') }}</button></p>
      <p v-if="profileWarning" class="share-notice share-profile-warning" role="status">{{ t('share.partialProfile') }} <button type="button" :disabled="loading || exporting || importing" @click="load">{{ t('share.retry') }}</button></p>
      <p v-if="imageWarning"  class="share-notice" role="status">{{ t('share.imageWarning') }}</p>
      <p v-if="localAddress" class="share-notice">{{ t('share.localAddress') }}</p>
      <div class="share-controls">
        <label class="share-import" :class="{ disabled: loading || exporting || importing }">{{ importing ? t('share.importing') : t('share.import') }}<input type="file" accept="image/jpeg,image/png,image/webp" :disabled="loading || exporting || importing" @change="importBackground"></label>
        <button type="button" :disabled="loading || exporting || importing" @click="resetBackground">{{ t('share.reset') }}</button>
        <button type="button" class="share-export" :disabled="loading || statsLoading || exporting || importing || loadFailed || !qr" @click="exportCard">{{ exporting ? t('share.exporting') : t('share.export') }}</button>
      </div>
      <p class="share-note">{{ t('share.backgroundHint') }} {{ t('share.statsHint') }}</p>
      <div v-if="preview" class="share-export-result" role="status"><p>{{ t('share.saved') }}</p><a :href="preview" target="_blank" rel="noopener">{{ t('share.openImage') }}</a><img :src="preview" :alt="t('share.previewAlt')"></div>
    </dialog>
  </Teleport>
</template>

<style scoped>
.food-share-dialog{width:min(1280px,calc(100vw - 32px));max-height:calc(100dvh - 32px);padding:24px;border:1px solid var(--border-paper);background:var(--color-paper);color:var(--color-ink);box-shadow:0 24px 70px #4b2f1f33;box-sizing:border-box;overflow:auto}
.food-share-dialog::backdrop{background:#30231ccc;backdrop-filter:blur(5px)}
.share-toolbar{display:flex;justify-content:space-between;align-items:center}.share-toolbar small{font-size:10px;letter-spacing:3px;color:var(--color-accent-label)}.share-toolbar h2{margin:5px 0;font-size:25px}.share-toolbar>button{background:none;border:0;font-size:32px;cursor:pointer;color:inherit}
.share-intro,.share-note{font-size:13px;line-height:1.7;color:#7c6756}.share-viewport{width:100%;aspect-ratio:1200/840;overflow:hidden;background:var(--color-bg)}.share-scale{width:1200px;height:840px;transform-origin:top left}
.share-card{position:relative;isolation:isolate;display:grid;grid-template-columns:780px 348px;grid-template-rows:466px 302px;gap:24px;width:1200px;height:840px;box-sizing:border-box;padding:24px;color:var(--color-ink);overflow:hidden;background:radial-gradient(ellipse at 10% 25%,#fffdf8 0,transparent 55%),linear-gradient(120deg,#fffaf0,#f5f0e5 58%,#f1e5d2);font-family:Arial,"Microsoft YaHei",sans-serif;text-align:left;line-height:1.4}
.share-card *{box-sizing:border-box}.share-card p{margin:0}.share-art,.share-shade{position:absolute;inset:0;width:100%;height:100%;z-index:-2;object-fit:cover}.share-shade{z-index:-1;background:linear-gradient(90deg,#fbf8f0d9,#f5ead8ed);border-top:5px solid var(--color-accent)}
.share-dish{grid-row:1 / 3;padding:8px 10px;min-width:0}.share-kicker{display:flex;justify-content:space-between;align-items:center;font-size:12px;letter-spacing:2px;color:#826c5b}.share-kicker span:first-child{padding:4px 8px;color:var(--color-paper);background:var(--color-accent)}
.share-dish-image{width:100%;aspect-ratio:16 / 9;margin:12px 0;display:grid;place-items:center;background:#efe1c9;border:1px solid var(--border-paper);overflow:hidden}.share-dish-image img{width:100%;height:100%;object-fit:cover}.share-dish-image span{font-size:18px;letter-spacing:5px;color:#826c5b}
.share-region{font-size:13px;letter-spacing:3px;color:var(--color-accent-label)}.share-dish h2{margin:10px 0 12px;font-size:36px;line-height:1.25;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden;overflow-wrap:anywhere}
.share-summary{font-size:16px;line-height:1.7;color:#6c5547;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden;overflow-wrap:anywhere}
.share-ingredients{margin-top:14px;border-top:1px solid var(--border-paper);padding-top:12px}.share-ingredients small{font-size:11px;letter-spacing:2px;color:var(--color-accent-label)}.share-ingredients p{font-size:14px;line-height:1.6;margin-top:5px;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden;overflow-wrap:anywhere}.share-credit{font-size:11px;color:#826c5b;margin-top:12px!important;white-space:nowrap;text-overflow:ellipsis;overflow:hidden}
.share-person{display:grid;grid-template-rows:94px 54px 80px 202px;gap:12px;padding-top:0;min-width:0}.share-panel{background:#fbf8f0e6;border:1px solid var(--border-paper);border-radius:4px;overflow:hidden}
.share-identity{display:flex;align-items:center;padding:12px;gap:12px;background:linear-gradient(120deg,#fffaf0,#f1e5d2)}.share-avatar{flex-shrink:0;width:52px;height:52px;border:2px solid #b89878;padding:3px;display:grid;place-items:center;font-size:26px;background:#efe1c9}.share-avatar img{width:100%;height:100%;object-fit:cover}.share-identity>div:last-child{min-width:0}.share-identity small{font-size:11px;letter-spacing:3px;color:var(--color-accent-label)}.share-identity h3{margin:4px 0;font-size:18px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.share-identity p{font-size:12px;color:#826c5b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}

.share-signature{padding:2px 12px;font-size:13px;line-height:20px;color:#826c5b;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden;overflow-wrap:anywhere}
.share-stats{display:grid;grid-template-columns:1fr 1fr;padding:16px 22px}.share-stats>div+div{border-left:1px solid var(--border-paper);padding-left:24px}.share-stats small{display:block;font-size:12px;letter-spacing:2px;color:#7c6756}.share-stats strong{color:var(--color-accent);font-size:26px;font-weight:500;letter-spacing:-1px;font-variant-numeric:tabular-nums}
.share-seals{display:flex;align-items:center;gap:14px;padding:10px 14px;background:#fffbf3e6}.share-seals-heading{display:flex;justify-content:space-between;color:var(--color-accent-label)}.share-seals-heading small{color:var(--color-accent-label);font-size:12px;letter-spacing:3px}.share-seals-heading span{font-size:18px;color:var(--color-accent)}.share-seal-grid{flex:1;min-width:0}.share-seal{display:flex;align-items:center;gap:10px;min-width:0;text-align:left}.share-seal img,.share-seal :deep(svg),.share-seal-fallback{display:block;width:78px;height:78px;object-fit:contain;margin:0 auto}.share-seal-fallback{font-size:55px;color:#9e6d55}.share-seal small{display:block;font-size:10px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:#7c6756}.share-seal.selected small{color:var(--color-accent)}.share-empty{padding:0;text-align:left;font-size:12px;line-height:1.8;color:#826c5b}
.share-rail{border-top:1px solid var(--border-paper);padding:14px 0 0;display:flex;flex-direction:column;align-items:center;justify-content:space-between;text-align:center}.share-rail strong{color:var(--color-accent);font-family:serif;font-size:26px;letter-spacing:2px;display:block}.share-rail>div>small{display:block;font-size:10px;letter-spacing:2px;color:#826c5b;margin-top:6px}.share-rail time{display:block;font-size:14px;color:#6c5547;margin-top:22px}.share-qr{width:100%}.share-qr img{display:block;width:160px;height:160px;max-width:100%;margin:auto;background:white}.share-qr p{font-size:15px;letter-spacing:2px;margin-top:8px}.share-qr small{font-size:11px;line-height:1.7;color:#7c6756;display:block;margin-top:7px}.share-rail-bottom{font-size:10px;letter-spacing:3px;color:#826c5b}
.share-controls{display:flex;gap:10px;flex-wrap:wrap;margin-top:20px;align-items:center}.share-controls button,.share-import{font:inherit;font-size:14px;padding:11px 17px;border:1px solid var(--border-paper);background:var(--color-input);color:var(--color-ink);cursor:pointer}.share-import{position:relative;overflow:hidden}.share-import input{position:absolute;inset:0;width:100%;height:100%;opacity:0;cursor:pointer}.share-import:focus-within{outline:2px solid var(--color-accent)}.share-controls .share-export{margin-left:auto;background:var(--color-accent);color:white;border-color:var(--color-accent)}.share-controls button:disabled,.share-import.disabled{opacity:.5;cursor:wait}.share-notice{font-size:13px;line-height:1.6;color:#7c6756}.share-error{color:var(--color-accent)}.share-export-result{padding-top:12px;font-size:13px}.share-export-result img{display:block;width:100%;margin-top:12px}.share-export-result a{color:var(--color-accent)}

/* Warm dossier panel: translucent information strips over an engraved bronze field. */
.share-card::after{content:"";position:absolute;z-index:-1;top:24px;right:24px;bottom:24px;width:348px;background:linear-gradient(145deg,#624337,#30231c 66%,#542c25);border-top:3px solid #b89878}
.share-person{padding:12px 12px 0;gap:12px;grid-template-rows:82px 54px 80px 202px}
.share-panel{border-color:#e5d4b745;border-radius:2px;box-shadow:0 4px 10px #160c0822}
.share-identity{position:relative;background:linear-gradient(115deg,#fbf8f0f5,#ead9bee8);border-left:3px solid var(--color-accent)}
.share-identity::after{content:"";position:absolute;right:10px;top:10px;width:20px;height:20px;border-top:1px solid #9e6d55;border-right:1px solid #9e6d55}
.share-identity small{font-size:10px;letter-spacing:2px}
.share-signature{margin:0!important;padding:7px 12px 7px 30px;position:relative;background:#fbf8f0de;border-left:2px solid #b89878;color:#6c5547;font-size:12px;line-height:19px}
.share-signature::before{content:"“";position:absolute;left:10px;top:1px;font-family:serif;font-size:28px;color:#9e6d55}
.share-stats{padding:9px 16px;background:#fbf8f0e8}
.share-stats>div+div{padding-left:18px}
.share-stats strong{font-size:30px}
.share-stats small{font-size:10px}
.share-seals{position:relative;display:block;padding:12px 16px;background:radial-gradient(circle at 50% 58%,#b898782b,transparent 65%),#211914a8}
.share-seals::before{content:"";position:absolute;left:50%;top:50%;width:136px;height:136px;transform:translate(-50%,-42%) rotate(30deg);border:1px solid #b898783d;border-radius:50%;pointer-events:none}
.share-seals-heading small{color:#d9c6a6;font-size:10px;letter-spacing:3px}
.share-seal{position:relative;display:flex;flex-direction:column;gap:0;margin-top:8px}
.share-seal img,.share-seal :deep(svg),.share-seal-fallback{width:122px;height:122px;object-fit:contain;margin:0 auto}
.share-seal small,.share-seal.selected small{color:#e6d4b4;font-size:11px;max-width:260px}
.share-empty{padding:42px 10px;color:#d5bfa4;text-align:center}
.share-rail{position:relative;margin:0 12px 12px;padding:14px 8px;display:grid;grid-template-columns:minmax(0,1fr) 166px;grid-template-rows:1fr 28px;gap:10px;align-items:center;border-top:1px solid #d9c6a650;color:#f5ead8}
.share-rail strong{font-size:21px;line-height:1.6;letter-spacing:1px;color:#f5ead8}
.share-rail>div>small,.share-rail time,.share-qr small{color:#cdb79a}
.share-rail time{font-size:11px;margin-top:14px}
.share-qr{position:relative}
.share-qr::before,.share-qr::after{content:"";position:absolute;width:14px;height:14px;border-color:#d2af77;border-style:solid;pointer-events:none}
.share-qr::before{left:-4px;top:-4px;border-width:2px 0 0 2px}
.share-qr::after{right:-4px;top:150px;border-width:0 2px 2px 0}
.share-qr img{width:160px;height:160px;border-radius:3px;box-shadow:0 4px 16px #170e0a55}
.share-qr p{font-size:13px;margin-top:12px}
.share-qr small{font-size:9px;letter-spacing:1px}
.share-rail-bottom{grid-column:1 / -1;border-top:1px solid #d9c6a630;padding-top:12px;color:#bfa98a;font-size:9px;letter-spacing:3px}

@media(max-width:600px){.food-share-dialog{padding:14px;width:calc(100vw - 16px);max-height:calc(100dvh - 16px)}.share-controls{gap:8px}.share-controls button,.share-import{padding:10px 12px;font-size:12px}.share-toolbar h2{font-size:21px}.share-controls .share-export{width:100%;margin:0}.share-intro{font-size:12px}}
</style>
