export interface ProfileStats {
  viewedFoodCount: number
  favoriteCount: number
}

export interface Region {
  id: number
  name: string
  province: string
  description: string
  centerLatitude?: number
  centerLongitude?: number
}

export interface UserSummary {
  id: number | null
  username: string
  displayName: string
  avatarUrl: string | null
}

export interface UserPublic {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
  signature: string | null
  selectedAchievement: Achievement | null
  selectedEtching: EtchingDesign | null
  foods: Food[]
}

export interface Food {
  id: number
  name: string
  region: Region
  latitude: number
  longitude: number
  address?: string
  summary: string
  story: string
  ingredients: string
  imageUrl?: string
  remark?: string
  heat: number
  reviewStatus: FoodReviewStatus
  reviewedBy?: string
  reviewedAt?: string
  createdBy: string
  creator: UserSummary
  createdAt: string
}

export interface FoodComment {
  id: number
  foodId: number
  author: UserSummary
  content: string
  createdAt: string
  checkinId?: number
  eatenOn?: string
}

export interface FoodCommentCreatePayload {
  content: string
}

export interface FoodCheckin {
  id: number
  foodId: number | null
  foodName: string
  eatenOn: string
  note?: string
  visibility: 'PUBLIC' | 'PRIVATE'
  timezone: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface PagedCheckins { items: FoodCheckin[]; total: number; page: number; pageSize: number }

export interface FoodTag {
  id: number
  type: 'TASTE' | 'INGREDIENT' | 'CUISINE'
  name: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'MERGED' | 'DISABLED'
  mergedIntoId?: number
  version: number
  createdBy?: number
  createdAt: string
  reviewedAt?: string
}
export interface PagedFoodTags { items: FoodTag[]; total: number; page: number; pageSize: number }
export interface FoodTagAdminPayload { type: FoodTag['type']; name: string; status: FoodTag['status']; reason?: string; version: number }

export interface FoodFootprint {
  food: Food
  visitedAt: string
}

export interface AgentClientAction {
  type: 'SWITCH_MUSIC' | 'COMMENT_PUBLISHED'
  query: string
}

export interface AgentChatPayload {
  message: string
  availableTracks: string[]
  currentFoodId?: number
}

export interface AgentChatResponse {
  reply: string
  clientAction?: AgentClientAction
  recommendations?: AgentFoodRecommendation[]
  commentDraft?: AgentCommentDraft
}

export interface AgentFoodRecommendation {
  id: number
  name: string
}

export interface AgentCommentDraft {
  foodId: number
  foodName: string
  content: string
}

export interface MapBounds {
  minLatitude: number
  maxLatitude: number
  minLongitude: number
  maxLongitude: number
}

/** 地图标记：后端 /foods/markers 的轻量载荷，只含弹窗所需字段。 */
export interface FoodMarker {
  id: number
  name: string
  region: Region
  latitude: number
  longitude: number
  summary: string
}

/** 目录分页：后端 /foods/catalog 的返回结构。 */
export interface PagedCatalog {
  items: Food[]
  total: number
  page: number
  pageSize: number
}

export interface FoodMapResults {
  items: FoodMarker[]
  total: number
  truncated: boolean
}

export type FoodSort = 'RELEVANCE' | 'HEAT' | 'NEWEST'

export interface MapCoordinate {
  latitude: number
  longitude: number
}

export interface MapFocus extends MapCoordinate {
  zoom: number
}

export interface PagedFoods {
  items: Food[]
  total: number
  page: number
  pageSize: number
  totalHeat: number
  pendingTotal: number
}

export type FoodReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface FoodLikeStatus {
  likeCount: number
  likedByMe: boolean
}

export interface FavoriteStatus {
  favorited: boolean
}

export type WishlistMatchField =
  | 'NAME'
  | 'INGREDIENTS'
  | 'REGION'
  | 'SUMMARY'
  | 'STORY'
  | 'ADDRESS'
  | 'DIRECT'

export interface WishlistMatch {
  food: Food
  score: number
  matchedFields: WishlistMatchField[]
}

export interface WishlistItem {
  id: number
  content: string
  sourceFoodId?: number
  createdAt: string
  matches: WishlistMatch[]
}

export interface WishlistItemCreatePayload {
  content?: string
  foodId?: number
}

export interface WishlistStatus {
  listed: boolean
}

export interface FoodReviewPayload {
  status: Extract<FoodReviewStatus, 'APPROVED' | 'REJECTED'>
}

export interface FoodCreatePayload {
  name: string
  province?: string
  city?: string
  regionId?: number
  latitude: number
  longitude: number
  address?: string
  summary: string
  story: string
  ingredients: string
  imageUrl?: string
  remark?: string
  tagIds?: number[]
}

export type FoodUpdatePayload = FoodCreatePayload

export type UserRole = 'USER' | 'SUB_ADMIN' | 'ADMIN'
export type LoginRole = Extract<UserRole, 'USER' | 'ADMIN'>

export interface AuthUser {
  id: number
  username: string
  displayName: string
  email: string | null
  avatarUrl?: string
  signature?: string
  signaturePending?: string
  signatureStatus?: SignatureStatus
  role: UserRole
  active: boolean
  createdAt: string
  pendingReviews?: PendingReview[]
}

export type ReviewField = 'DISPLAY_NAME' | 'SIGNATURE' | 'SEAL'
export type ReviewItemStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface PendingReview {
  id: number
  field: ReviewField
  currentValue: string
  pendingValue: string
  requestedAt: string
}

export interface ReviewItemPayload {
  field: ReviewField
  status: Extract<ReviewItemStatus, 'APPROVED' | 'REJECTED'>
}

export type SignatureStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface Achievement {
  id: number
  code: string
  name: string
  description: string
  imageUrl: string
  unlockedAt: string
  selected: boolean
}

export interface EtchingDesign {
  id: number
  name: string
  layerOne: string[]
  selected: boolean
  createdAt: string
  updatedAt: string
}

export interface EtchingDesignPayload {
  name: string
  layerOne: string[]
}
export interface LoginPayload {
  username: string
  password: string
  role: LoginRole
}

export interface RegisterPayload {
  username: string
  password: string
  displayName: string
  email: string
  verificationCode: string
}

export interface CaptchaChallenge {
  captchaId: string
  question: string
}

export interface SendRegistrationCodePayload {
  email: string
  captchaId: string
  captchaAnswer: string
}

export interface SendPasswordResetCodePayload {
  username: string
  email: string
}

export interface PasswordResetPayload extends SendPasswordResetCodePayload {
  verificationCode: string
  newPassword: string
}
export interface SetUserActivePayload {
  active: boolean
}

export interface SetUserRolePayload {
  role: Extract<UserRole, 'USER' | 'SUB_ADMIN'>
}

export interface FoodImportIssue {
  rowNumber: number
  reason: string
}

export interface FoodImportResult {
  totalRows: number
  importedCount: number
  skippedCount: number
  duplicateCount: number
  anonymousCount: number
  invalidCount: number
  truncatedCount: number
  issues: FoodImportIssue[]
}

export interface PagedAuthUsers {
  items: AuthUser[]
  total: number
  page: number
  pageSize: number
}
