import { ref } from 'vue'
import zh from './locales/zh-CN'
import en from './locales/en-US'
export const locale = ref(uni.getStorageSync('terra-language') === 'en' ? 'en' : 'zh')
export function t(key: keyof typeof zh): string { return (locale.value === 'en' ? en : zh)[key] }
export function toggleLanguage() {
  locale.value = locale.value === 'zh' ? 'en' : 'zh'
  uni.setStorageSync('terra-language', locale.value)
  uni.setTabBarItem({ index: 0, text: locale.value === 'zh' ? '寻味' : 'Discover' })
  uni.setTabBarItem({ index: 1, text: locale.value === 'zh' ? '我的' : 'Journal' })
}
