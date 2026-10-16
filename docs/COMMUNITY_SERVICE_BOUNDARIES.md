# 社区服务接口与数据边界

在 `feat/service-split-mobile-clients` 上进一步拆分社区进程。外部 URL、HTTP 方法、状态码、响应字段、分页、登录 Session、CSRF 和账号切换校验保持原样；Web、App、小程序继续只访问 Gateway。

## 运行与依赖

| 应用 | 默认端口 | 业务实现 |
| --- | --- | --- |
| food-gateway | 8080 | 统一入口，按具体路径转发，不重写路径、不自动重试写请求 |
| food-core-boot | 8081 | catalog、agent；仅此服务运行 Flyway |
| food-media-boot | 8082 | media |
| food-user-boot | 8083 | user、achievement，登录和账号生命周期 |
| food-community-boot | 8084 | community，使用 user-client 查询账号状态和公开资料 |

社区不依赖 `food-catalog` 实现，不包含菜品 CRUD、导入、用户管理或图片处理组件。核心也不再依赖 `food-community`。社区可独立重启、构建和扩容；Redis Session 命名空间和 `AppUserPrincipal` 序列化类不变。兼容启动 `food-boot` 仍组装所有本地模块，并使用本地用户资料查询。

## 接口归属

| 原路径 | 归属 | 功能 |
| --- | --- | --- |
| `/api/profile/favorites`、`/page`、`/{foodId}`、`/{foodId}/status` | 社区 | 收藏列表、分页、添加、移除、状态 |
| `/api/profile/wishlist`、`/page`、`/{id}`、`/foods/{foodId}/status` | 社区 | 心愿单、匹配、分页、删除、状态 |
| `/api/foods/{foodId}/comments`、`/page` | 社区 | 评论列表、分页、发表 |
| `/api/foods/{foodId}/like`、`/status`、`/unlike` | 社区 | 点赞、查询、取消 |
| `/api/foods/{foodId}/check-ins` | 社区 | 创建打卡（保留 Idempotency-Key） |
| `/api/profile/check-ins`、`/{id}` | 社区 | 个人打卡列表、详情、编辑、删除 |
| `/api/profile/stats` | 社区 | 已浏览菜品和收藏统计 |
| `/api/users/{id}` | 社区 | 用户公开资料与已审核投稿聚合 |
| `/api/foods/**` 的其他已有操作、`/api/profile/foods`、`/api/profile/footprints`、`/api/admin/foods/**` | 核心 | 菜品和浏览足迹 |
| `/api/auth/**`、用户资料字段、`/api/admin/users/**`、成就与刻蚀 | 用户 | 原用户服务接口 |
| `/api/images/**`、`/uploads/**` | 图片 | 原图片服务接口 |

网关先匹配具体社区路径，再进入核心兜底。不能把全部 `/api/foods/**`、`/api/profile/**` 或 `/api/users/**` 管理路径笼统分发；公开主页 `/api/users/{id}` 与管理接口 `/api/admin/users/**` 是不同归属。

## 数据边界与事务

当前是**独立进程、共享数据库的过渡架构**，没有完成分库。

社区拥有 `food_favorite`、`wishlist_item`、`food_comment`、`food_like`、`food_checkin` 和打卡幂等记录的业务写入。具体表名以现有 Mapper/迁移为准；本轮不新增或改写任何历史 SQL。打卡发布与关联评论创建仍在一个本地事务中；编辑时公开/私密切换、乐观版本冲突回滚、删除关联评论、幂等重放及删除后禁止重发均保留。

明确保留的跨域依赖：

- `CommunityFoodMapper` 是社区自己的只读投影，仅提供已审核菜品详情、按 ID 批量读取、公开投稿、心愿匹配候选和发现版本读取，没有菜品写接口。SQL 与原查询的字段、排序、数量限制、审核/归属过滤保持一致。收藏列表的 resultMap/字段也由该投影提供，不再引用 catalog 的 Mapper XML。真实 MySQL 回归对照两侧查询结果，防止投影字段漂移。
- 心愿匹配通过 `CommunityCatalogVersion.current()` 新事务读取已提交的 `public_discovery_version`。核心提交菜品变化后，社区的本地匹配缓存使用新版本键；查询中途版本变化仍返回原有 409。社区写事务持有用户行锁时继续跳过该缓存，避免再次获取连接和锁顺序变化。
- `AppUserMapper` 仍在 platform。社区业务写入通过 `AuthenticatedActor` 在本地事务中锁定用户行，与用户服务停用、改角色、删除保持原有互斥。每个已登录请求另由 user-client 验证账号状态。用户服务不可用时返回 503，保留 Session；匿名评论/点赞状态读取仍可用。
- 评论作者、收藏、心愿匹配和浏览统计仍有共享表读取；公开主页的用户资料使用用户服务内部 HTTP 契约，菜品部分使用只读投影。媒体变体 URL、外键及账号/菜品删除联动保持现状。

后续拆库需要替换以上投影、联表与外键，并设计账号撤销、菜品可见性及删除传播的一致性协议。不能直接用多个 HTTP 请求替换打卡/评论事务或用户行锁。当前没有引入跨服务写调用、分布式事务、消息队列或注册中心。

## 启动、发布与回退

1. 使用原 MySQL、Redis 和上传目录；core、user、media、community 配置相同 `USER_SERVICE_TOKEN`（至少 32 字符）。各服务的 Session 设置一致。
2. 先启动 core 并等待 Flyway 完成，再启动 user、media、community，最后启动 gateway。社区默认只监听 `127.0.0.1:8084`，只通过受限内部网络提供服务。用户内部契约及网关拦截规则沿用 [用户服务说明](USER_SERVICE_BOUNDARIES.md)。
3. 网关新增 `COMMUNITY_SERVICE_URL`，默认 `http://127.0.0.1:8084`。Compose 中配置为 `http://community:8084`。统一部署入口仍为 `ops/services/compose.yml`；Dockerfile 增加 community target。
4. 本地 `ops/local/start.ps1` 会启动新增社区进程 `18084`，对外仍使用网关 `18080` 和网页 `5173`；复用 `.local` 配置与数据卷。原本地注册邮箱验证开关仍仅作用于用户服务。
5. 发布时同步更新核心、社区和网关：新核心已移除社区 Controller，不能让旧网关继续将社区请求送往新核心。先准备好新社区，再切换匹配的新网关与核心；切换期间安排短暂维护窗口或完整一组服务进行流量切换。
6. 回退可停止拆分进程并运行兼容 `target/dayanfood-backend-0.0.1-SNAPSHOT.jar`，接回相同外部入口。没有 schema 或 Session 格式变更。避免同时启动两套图片维护任务。

## 验证方式

`mvn -f dayanfood-backend/pom.xml clean verify` 运行原有回归、社区/核心 JAR 隔离测试和网关路由矩阵。原有真实 MySQL/Redis 用例按现有环境变量显式启用，覆盖并发重试、权限、事务、升级及只读投影一致性。

额外的 `python ops/services/verify_community.py` 会自行启动五个真实 JAR，检查原路径、共享登录、收藏/心愿单/点赞/评论、并发打卡幂等、版本冲突回滚、公开私密切换、越权、停用撤销、跨进程缓存失效、用户服务故障、社区独立重启和核心停机后的社区读取。所有启动的进程都会在退出时停止。

运行脚本前必须提供隔离测试基础设施，设置 `DB_URL`（仅接受 localhost/127.0.0.1 上 `terra_audit_*` 库）、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、必要的 `REDIS_PASSWORD`。MySQL 使用 REPEATABLE READ；不要指向开发业务 Redis。可设置 `JAVA` 和 `COMMUNITY_TEST_OUTPUT`。默认日志和结果写入被忽略的 `.local/community-validation`；脚本会创建测试账号与菜品，不删除或重置现有数据，应使用可丢弃的测试库。

本次执行结果见 [社区服务验证记录](COMMUNITY_SERVICE_VALIDATION.md)。
