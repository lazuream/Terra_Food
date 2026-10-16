# 用户服务接口与数据边界

本轮拆分建立在 `feat/service-split-mobile-clients` 上。原有外部 URL、HTTP 方法、响应结构、Session Cookie 和 CSRF 协议继续使用。Web、App、微信小程序仍只访问 Gateway。

## 运行与模块依赖

- `food-user-boot`（8083）：依赖 `food-user`、`food-achievement`、`food-platform`；持有登录认证提供器。没有菜品、社区、媒体业务实现。
- `food-core-boot`（8081）：依赖 catalog、agent、user-client；不打入 user/achievement 实现。
- `food-media-boot`（8082）：依赖 media、user-client；导出图片仍由核心验证菜品可见性。
- `food-community-boot`（8084）：依赖 community、user-client，承接社区接口，详见[社区服务边界](COMMUNITY_SERVICE_BOUNDARIES.md)。
- `food-gateway`（8080）：具体用户、社区、图片路径优先，其他 `/api/**` 到核心。
- `food-boot`：继续组装全部本地模块，使用本地查询适配器，不调用内部 HTTP，也不要求新增服务令牌。

所有服务继续使用现有 MySQL、Redis Session 命名空间与 `AppUserPrincipal` 类。没有切换 JWT，没有增加注册中心、消息队列或分布式事务。

## 外部接口归属

| 原路径 | 处理方 | 说明 |
| --- | --- | --- |
| `/api/auth/**` | 用户 | 登录、注册、图形验证码、邮件验证、密码重置、me、logout、CSRF |
| `/api/admin/users`、`/api/admin/users/**` | 用户 | 列表、停用、角色、资料审核、删除 |
| `/api/profile/avatar`、`signature`、`display-name` | 用户 | 原资料更新方法和响应不变 |
| `/api/achievements/**`、`/api/etchings/**`（含根路径） | 用户 | 选中成就与刻蚀互斥，保留同一事务 |
| `/api/profile/foods`、`/api/profile/foods/{id}`、`/api/profile/footprints` | 核心 | 从原 ProfileController 移至 ProfileFoodController |
| `/api/admin/foods`、`/api/admin/foods/{id}/review` | 核心 | 从原 AdminController 移至 AdminFoodController |
| `/api/users/{id}` | 社区 | 调用用户公开资料接口，再聚合本地已审核菜品 |
| 收藏、心愿单、评论、点赞、打卡、统计 | 社区 | 原本地事务与 API 保留 |
| 菜品和地区接口 | 核心 | 原本地事务与 API 保留 |
| `/api/images/**`、`/uploads/**` | 图片 | 原上传、导出、静态文件入口保留 |

不能用整个 `/api/profile/**` 或 `/api/admin/**` 路由到用户服务，这两个前缀跨业务域。

## 内部契约

契约为 `AccountStatusReader` 与 `PublicUserProfileReader`。兼容启动使用本地实现；核心、媒体和社区使用 `food-user-client` 的 HTTP 实现。

| 内部 GET 接口 | 返回字段 | 用途 |
| --- | --- | --- |
| `/api/internal/users/{id}/identity` | subjectId、authVersion、active、role | 每次受保护请求校验 Session 是否仍有效 |
| `/api/internal/users/{id}/public-profile` | id、username、displayName、avatarUrl、signature、selectedAchievement、selectedEtching | 公开主页聚合 |

内部接口不返回密码哈希、邮箱或审核中的资料。身份查询 SQL 仅选择四个必要字段。公开资料只读已公开字段；停用/不存在账号返回 404。

内部调用要求 `X-User-Service-Token`，来自相同的 `USER_SERVICE_TOKEN`（随机值、至少 32 字符）。令牌仅用于服务读接口，不代表用户身份；不转发调用者 Cookie。令牌空缺/过短时启动失败；服务端使用恒定时间比较。Gateway 屏蔽全部内部路径并移除客户端同名头。部署必须限制业务服务为内部网络，只有网关对外；若跨不可信网络部署，需要为内部地址配置 TLS 或受保护隧道。

连接超时 2 秒、读取超时 3 秒，不跟随重定向、不缓存账号状态、不自动重试。404 身份查询视为账号失效；认证失败、网络故障、无效响应及 5xx 视为依赖不可用，受保护请求返回 503，保留原 Session。匿名菜品浏览不依赖账号状态查询；公开用户主页仍依赖用户服务。

## 数据所有权及明确保留的过渡依赖

| 数据 | 逻辑归属 | 本轮处理 |
| --- | --- | --- |
| app_user、user_review_item、验证码兑换记录 | 用户 | 注册、密码、角色、停用、删除、资料审核均由用户业务执行 |
| 成就、用户成就、刻蚀设计 | 用户 | 与账号一起部署，保留成就/刻蚀选择互斥事务 |
| 菜品、地区、标签、浏览足迹、发现版本、菜品幂等记录 | 核心 | 保留现有查询、写入与权限规则 |
| 收藏、心愿单、评论、点赞、打卡及其幂等/删除标记 | 社区 | 打卡与关联评论保留原事务 |
| 图片资产与上传文件 | 图片 | 保留归属和引用检查 |
| Redis Session、限流及验证挑战 | 共享基础设施，用户主导登录生命周期 | Session 的序列化格式和命名空间不变 |
| 外部记忆删除任务 | 用户登记、既有 Agent 流程消费 | 删除账号时的任务登记与删除继续原子提交 |

**当前不是独立数据库架构。** `AppUserMapper` 暂留 platform，业务写入仍通过 `AuthenticatedActor` 在本地数据库事务中锁定用户行，防止停用、改角色或删除与菜品/社区写入竞争。仅用 HTTP 校验替代行锁，会留下“校验成功后立刻被停用仍可写入”的竞态，本轮没有这样替换。

其余保留项：核心的用户摘要/权限读取及部分 SQL 联表；图片清理对头像引用的检查；现有用户外键及删除联动。共享 Mapper 仍包含账号读写方法，是过渡技术债，不代表任意服务可新增账号管理业务。新增账号管理写入应放在用户模块。跨域 PO 仍存在于 common，只允许窄 VO 经过本轮内部 HTTP 接口。

后续拆库前需要逐项替换这些引用为事件投影/查询契约，并设计用户撤销与业务写入的一致性协议；不能直接去掉外键或把原事务改成多次 HTTP 请求。

## 发布与回退

1. 使用原数据库、Redis 和上传目录；生成并一致配置 `USER_SERVICE_TOKEN`。
2. 先运行 core，等待现有 Flyway 完成，再启动 user、media、community、gateway。所有 31 份历史迁移保持原样，仅 core（或兼容模式 boot）执行迁移。
3. `ops/services/compose.yml` 提供五服务统一部署；新数据库首次部署建议先 `up -d core`，确认迁移完成后再 `up -d`。生产数据库连接、Redis 配置、Session cookie secure 属性在各服务保持一致。邮件与初始管理员环境变量放到 user。
4. 验证网关登录、跨服务菜品/图片写入、CSRF、账号切换、停用撤销、退出及公开主页。
5. 回退可停止拆分进程，使用兼容 `target/dayanfood-backend-0.0.1-SNAPSHOT.jar` 接回同一入口。没有本轮新增 schema 或 Session 格式迁移；避免端口冲突和重复启动维护任务。

用户服务拆分阶段的验证记录见 [用户服务拆分验证](USER_SERVICE_VALIDATION.md)。
