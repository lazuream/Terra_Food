import { createI18n } from 'vue-i18n'

import enUS from './locales/en-US'
import zhCN from './locales/zh-CN'

export type SupportedLocale = 'zh-CN' | 'en-US'

const STORAGE_KEY = 'dayan-food-locale'

function getInitialLocale(): SupportedLocale {
  // 用户主动选择的语言优先于浏览器设置，保证刷新后的语言体验一致。
  let savedLocale: string | null = null
  try { savedLocale = localStorage.getItem(STORAGE_KEY) } catch { /* private/storage-restricted context */ }
  if (savedLocale === 'zh-CN' || savedLocale === 'en-US') {
    return savedLocale
  }

  return navigator.language.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en-US'
}

export const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export function saveLocale(locale: SupportedLocale) {
  try { localStorage.setItem(STORAGE_KEY, locale) } catch { /* language still applies for this page */ }
  document.documentElement.lang = locale
}
