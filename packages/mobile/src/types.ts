export interface Region { id: number; name: string; province: string; description?: string }
export interface Food {
  id: number; name: string; region: Region; summary: string; story: string; ingredients: string;
  imageUrl?: string; imageVariants?: { small?: string; medium?: string }; address?: string; heat: number;
}
export interface Catalog { items: Food[]; total: number; page: number; pageSize: number }
export interface AuthUser { id: number; username: string; displayName: string; avatarUrl?: string }
export interface FavoriteStatus { favorited: boolean }
