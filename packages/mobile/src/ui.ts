import { ApiError } from './client'
import { t } from './i18n'
export function errorText(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'NOT_CONFIGURED') return t('notConfigured')
    if (error.status === 0) return t('network')
    if (error.status === 404) return t('missing')
  }
  return t('error')
}
export function openFood(id: number) { uni.navigateTo({ url: '/pages/detail/index?id=' + id }) }
export function openLogin() { uni.navigateTo({ url: '/pages/login/index' }) }
