<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import { createFood, uploadImage } from '../api'
import { apiErrorMessage } from '../apiError'
import { useAuth } from '../auth'
import FoodTagPicker from './FoodTagPicker.vue'
import {
  cacheDraftImage,
  clearDraft,
  forgetDraftImage,
  getCachedDraftImage,
  readDraft,
  saveDraft,
  type DraftImageMeta,
} from '../drafts'
import type { Food, FoodCreatePayload, Region } from '../types'

const props = defineProps<{
  regions: Region[]
  latitude?: number
  longitude?: number
  regionId?: number
  address?: string
  province?: string
  city?: string
}>()

const emit = defineEmits<{
  close: []
  saved: [food: Food]
}>()

// 坐标只能来自地图选点（通过 props 注入），表单自身不提供默认坐标，
// 避免未选点时带上无效经纬度直接创建。
type UploadForm = Omit<FoodCreatePayload, 'latitude' | 'longitude'> & {
  latitude?: number
  longitude?: number
}

const form = reactive<UploadForm>({
  name: '',
  regionId: undefined,
  province: '',
  city: '',
  latitude: undefined,
  longitude: undefined,
  address: '',
  summary: '',
  story: '',
  ingredients: '',
  remark: '',
  tagIds: [],
})

const image = ref<File>()
const imageMeta = ref<DraftImageMeta>()
const previewUrl = ref('')
const coverInput = ref<HTMLInputElement>()
const saving = ref(false)
const error = ref('')
const stage = ref<'idle' | 'uploading' | 'saving'>('idle')
const { t } = useI18n()
const auth = useAuth()
const accountId = auth.currentUser.value?.id
const DRAFT_KEY = `foodUpload.v2.${accountId}`
const DRAFT_TTL_MS = 7 * 24 * 60 * 60 * 1000
let uploadController: AbortController | undefined

watch(
  () => [props.latitude, props.longitude],
  ([latitude, longitude]) => {
    if (latitude != null && longitude != null) {
      form.latitude = Number(latitude.toFixed(7))
      form.longitude = Number(longitude.toFixed(7))
    }
  },
  { immediate: true },
)

watch(
  () => [props.province, props.city],
  ([province, city]) => {
    if (!form.province) form.province = province || ''
    if (!form.city) form.city = city || ''
  },
  { immediate: true },
)

watch(
  () => props.address,
  (address) => {
    form.address = address || ''
  },
  { immediate: true },
)

// 草稿缓存：文本字段在误关弹窗后保留；坐标/地区/地址永远跟随地图选点（props），不参与草稿。
interface UploadDraft {
  name: string
  summary: string
  ingredients: string
  story: string
  remark: string
  image?: DraftImageMeta
  imageUrl?: string
  idempotencyKey: string
  tagIds?: number[]
  expiresAt: number
}

let draft = readDraft<UploadDraft>(DRAFT_KEY)
if (draft && (!Number.isFinite(draft.expiresAt) || draft.expiresAt <= Date.now())) {
  clearDraft(DRAFT_KEY)
  draft = undefined
}
const idempotencyKey = ref(draft?.idempotencyKey || crypto.randomUUID())
if (draft) {
  form.name = draft.name
  form.summary = draft.summary
  form.ingredients = draft.ingredients
  form.story = draft.story
  form.remark = draft.remark
  form.imageUrl = draft.imageUrl
  form.tagIds = draft.tagIds || []
  if (draft.image) {
    imageMeta.value = draft.image
    const cachedImage = getCachedDraftImage(DRAFT_KEY)
    if (cachedImage) {
      image.value = cachedImage
      previewUrl.value = URL.createObjectURL(cachedImage)
    }
  }
}

function persistDraft() {
  saveDraft(DRAFT_KEY, {
    name: form.name,
    summary: form.summary,
    ingredients: form.ingredients,
    story: form.story,
    remark: form.remark,
    image: imageMeta.value,
    imageUrl: form.imageUrl,
    idempotencyKey: idempotencyKey.value,
    tagIds: form.tagIds,
    expiresAt: Date.now() + DRAFT_TTL_MS,
  })
}

watch(
  () => [{ ...form, tagIds: [...(form.tagIds || [])] }, imageMeta.value],
  () => {
    idempotencyKey.value = crypto.randomUUID()
    persistDraft()
  },
)

onBeforeUnmount(() => {
  uploadController?.abort()
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})

function selectImage(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  // 重置 input 使同一文件可再次触发 change；取消选择时不覆盖已选图片。
  input.value = ''
  if (!file) return

  const allowedTypes = new Set(['image/jpeg', 'image/png', 'image/webp'])
  if (file.size > 5 * 1024 * 1024) {
    error.value = t('upload.imageTooLarge')
    return
  }
  if (!allowedTypes.has(file.type)) {
    error.value = t('upload.imageInvalidType')
    return
  }

  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  image.value = file
  form.imageUrl = undefined
  idempotencyKey.value = crypto.randomUUID()
  imageMeta.value = { name: file.name, type: file.type, size: file.size }
  previewUrl.value = URL.createObjectURL(file)
  cacheDraftImage(DRAFT_KEY, file)
}

async function submit() {
  if (saving.value) return
  error.value = ''

  // 坐标必须来自地图选点，禁止带着默认值直接创建。
  if (form.latitude == null || form.longitude == null
      || !Number.isFinite(form.latitude) || !Number.isFinite(form.longitude)) {
    error.value = t('upload.coordinateRequired')
    return
  }

  saving.value = true
  try {
    // 图片与菜品信息分两步提交：先取得资源 URL，再保存稳定的业务记录。
    if (image.value && !form.imageUrl) {
      stage.value = 'uploading'
      uploadController = new AbortController()
      form.imageUrl = await uploadImage(image.value, uploadController.signal)
      uploadController = undefined
      persistDraft()
    }

    stage.value = 'saving'
    const submissionKey = idempotencyKey.value
    const payload = {
      ...form,
      latitude: form.latitude as number,
      longitude: form.longitude as number,
    }
    const food = await createFood(payload, submissionKey)
    if (food.reviewStatus === 'PENDING') {
      window.alert(t('upload.pendingSuccess'))
    }
    if (submissionKey === idempotencyKey.value) {
      clearDraft(DRAFT_KEY)
      forgetDraftImage(DRAFT_KEY)
    }
    emit('saved', food)
  } catch (requestError) {
    error.value = apiErrorMessage(requestError, stage.value === 'uploading'
      ? t('upload.imageUploadError')
      : t('upload.saveError'))
  } finally {
    saving.value = false
    stage.value = 'idle'
    uploadController = undefined
  }
}

function cancelUpload() {
  uploadController?.abort()
}
</script>

<template>
  <div class="modal-mask" @click.self="emit('close')">
    <section class="upload-modal">
      <div class="modal-title">
        <div>
          <small>{{ t('upload.eyebrow') }}</small>
          <h2>{{ t('upload.title') }}</h2>
        </div>
        <button class="icon-button" @click="emit('close')">×</button>
      </div>

      <form @submit.prevent="submit">
        <p v-if="stage !== 'idle'" class="form-status" role="status">
          {{ stage === 'uploading' ? t('upload.uploadingImage') : t('upload.savingFood') }}
          <button v-if="stage === 'uploading'" type="button" @click="cancelUpload">{{ t('upload.cancelUpload') }}</button>
        </p>
        <fieldset class="submit-snapshot" :disabled="saving">
        <div class="form-grid">
          <label>
            {{ t('upload.name') }}
            <input v-model="form.name" required maxlength="100">
          </label>
          <label>
            {{ t('upload.cityLabel') }}
            <input v-model.trim="form.province" maxlength="100" :placeholder="t('upload.provincePlaceholder')">
            <input v-model.trim="form.city" maxlength="100" :placeholder="t('upload.cityPlaceholder')">
          </label>
          <label>
            {{ t('upload.latitude') }}
            <input v-model.number="form.latitude" type="number" min="-90" max="90" step="0.0000001" required>
          </label>
          <label>
            {{ t('upload.longitude') }}
            <input v-model.number="form.longitude" type="number" min="-180" max="180" step="0.0000001" required>
          </label>
        </div>

        <p class="coordinate-tip">{{ t('upload.coordinateTip') }}</p>

        <label>
          {{ t('upload.address') }}
          <input v-model.trim="form.address" maxlength="500" :placeholder="t('upload.addressPlaceholder')">
        </label>

        <label>
          {{ t('upload.summary') }}
          <textarea v-model="form.summary" required maxlength="1000" rows="2"></textarea>
        </label>
        <label>
          {{ t('upload.ingredients') }}
          <input v-model="form.ingredients" required maxlength="500">
        </label>
        <FoodTagPicker v-model="form.tagIds" :disabled="saving" />
        <label>
          {{ t('upload.story') }}
          <textarea v-model="form.story" required rows="4"></textarea>
        </label>
        <label>
          {{ t('upload.remark') }}
          <textarea v-model="form.remark" maxlength="1000" rows="3" :placeholder="t('upload.remarkPlaceholder')"></textarea>
        </label>
        <div class="cover-field">
          <span>{{ t('upload.cover') }}</span>
          <div class="cover-row">
            <input
              ref="coverInput"
              class="hidden-file-input"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              :disabled="saving"
              @change="selectImage"
            >
            <button type="button" class="cover-pick" :disabled="saving" @click="coverInput?.click()">
              {{ image ? t('upload.changeCover') : t('upload.pickCover') }}
            </button>
          </div>
          <img v-if="previewUrl" class="cover-preview" :src="previewUrl" :alt="t('upload.cover')">
          <small v-if="imageMeta && !image" class="cover-warning">{{ t('upload.imageNeedsReselect') }}</small>
          <small>{{ t('upload.imageTip') }}</small>
        </div>
        </fieldset>

        <p v-if="error" class="form-error">{{ error }}</p>

        <div class="modal-actions">
          <button type="button" class="secondary-button" @click="emit('close')">
            {{ t('common.cancel') }}
          </button>
          <button class="primary-button" :disabled="saving">
            {{ saving ? t('upload.saving') : t('upload.submit') }}
          </button>
        </div>
      </form>
    </section>

  </div>
</template>
