import { createClient, ApiError } from './client'
import type { AuthUser, Catalog, FavoriteStatus, Food, Region } from './types'
import { demoFoods, demoRegions } from './demo'

export const demoMode = import.meta.env.VITE_DEMO_MODE === 'true'
const configured = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
export const apiConfigured = /^https?:\/\//.test(configured)
const storageKey = 'terra-session:' + configured
export const client = createClient((path, method, data, header) => new Promise((resolve, reject) => {
  if (!apiConfigured) return reject(new ApiError(0, 'NOT_CONFIGURED', 'API address missing'))
  uni.request({
    url: configured + path, method, data: data as Record<string, unknown>, header, timeout: 15000,
    success(response) {
      const headers = response.header as Record<string, string | string[]>
      const raw = headers['Set-Cookie'] || headers['set-cookie']
      const cookies = response.cookies?.length ? response.cookies : raw ? (Array.isArray(raw) ? raw : [raw]) : []
      resolve({ status: response.statusCode, data: response.data, cookies })
    },
    fail: () => reject(new ApiError(0, 'NETWORK_ERROR', 'Network unavailable')),
  })
}), {
  read: () => String(uni.getStorageSync(storageKey) || ''),
  write: value => value ? uni.setStorageSync(storageKey, value) : uni.removeStorageSync(storageKey),
})

export function imageUrl(value?: string): string {
  if (!value) return ''
  if (value.startsWith('/uploads/') && apiConfigured) return configured.replace(/\/api$/, '') + value
  return /^https?:\/\//.test(value) ? value : ''
}
export async function catalog(keyword = '', regionId = 0, page = 1): Promise<Catalog> {
  if (demoMode) {
    const items = demoFoods.filter(food => (!regionId || food.region.id === regionId) && (food.name + food.summary).includes(keyword))
    return { items: items.slice((page - 1) * 12, page * 12), total: items.length, page, pageSize: 12 }
  }
  return client.get(`/foods/catalog?page=${page}&pageSize=12&keyword=${encodeURIComponent(keyword)}${regionId ? '&regionId=' + regionId : ''}`)
}
export const regions = (): Promise<Region[]> => demoMode ? Promise.resolve(demoRegions) : client.get('/regions')
export async function food(id: number): Promise<Food> {
  if (!demoMode) return client.get(`/foods/${id}?recordVisit=false`)
  const result = demoFoods.find(item => item.id === id)
  if (!result) throw new ApiError(404, 'NOT_FOUND', 'Food not found')
  return result
}
export const favorites = (): Promise<Catalog> => client.get('/profile/favorites/page?page=1&pageSize=12')
export const favoriteStatus = (id: number): Promise<FavoriteStatus> => client.get(`/profile/favorites/${id}/status`)
export const setFavorite = (id: number, active: boolean): Promise<FavoriteStatus> => client.write(`/profile/favorites/${id}`, undefined, active ? 'POST' : 'DELETE')
export const login = (username: string, password: string): Promise<AuthUser> => client.login(username, password)
