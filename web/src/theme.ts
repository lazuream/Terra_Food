import { readonly, ref } from 'vue'

export type ThemeMode = 'system' | 'light' | 'dark'
const STORAGE_KEY = 'terra-food.theme'

function storedMode(): ThemeMode {
  try {
    const value = localStorage.getItem(STORAGE_KEY)
    return value === 'light' || value === 'dark' ? value : 'system'
  } catch { return 'system' }
}

const mode = ref<ThemeMode>(storedMode())
const media = window.matchMedia('(prefers-color-scheme: dark)')

function applyTheme() {
  const resolved = mode.value === 'system' ? (media.matches ? 'dark' : 'light') : mode.value
  document.documentElement.dataset.theme = resolved
  document.documentElement.style.colorScheme = resolved
}

export function setTheme(next: ThemeMode, persist = true) {
  mode.value = next
  applyTheme()
  if (!persist) return
  try { localStorage.setItem(STORAGE_KEY, next) } catch { /* storage can be unavailable */ }
}

media.addEventListener('change', () => { if (mode.value === 'system') applyTheme() })
window.addEventListener('storage', (event) => {
  if (event.key === STORAGE_KEY) setTheme(event.newValue === 'light' || event.newValue === 'dark' ? event.newValue : 'system', false)
})
applyTheme()

export function useTheme() { return { themeMode: readonly(mode), setTheme } }
