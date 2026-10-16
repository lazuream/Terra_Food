# 后端模块与服务启动

提供两种启动方式，保留原有 API、业务数据库、Redis Session 格式和原有单体 JAR 路径。

| 应用 | 入口 / 默认端口 | 职责 |
| --- | --- | --- |
| `food-boot` | `DayanFoodApplication` / 8080 | 兼容模式：全部业务在一个 JVM，仍可统一启动部署 |
| `food-gateway` | `GatewayApplication` / 8080 | Spring Cloud Gateway WebFlux，外部统一入口 |
| `food-core-boot` | `CoreApplication` / 8081 | 菜品与 Java Agent 业务 |
| `food-community-boot` | `CommunityApplication` / 8084 | 收藏、心愿单、评论、点赞、打卡、统计及公开主页 |
| `food-user-boot` | `UserApplication` / 8083 | 登录、账号、资料审核、成就与刻蚀 |
| `food-media-boot` | `MediaApplication` / 8082 | 独立图片上传、资产记录、导出与文件服务 |

拆分模式是 **网关 + 用户服务 + 核心服务 + 图片服务 + 社区服务**。用户服务独立运行，核心、媒体和社区通过内部接口查询账号状态，原有登录、Cookie、CSRF 和 API URL 保持不变。当前共享 MySQL 与 Redis Session，写入时的用户行锁、跨域外键仍保留；尚未实现按服务拆库。

接口归属、数据所有权和保留的跨域依赖见 [用户服务边界说明](../docs/USER_SERVICE_BOUNDARIES.md)及[社区服务边界说明](../docs/COMMUNITY_SERVICE_BOUNDARIES.md)。

## 模块

| 模块 | 职责 |
| --- | --- |
| `food-common` | 共享 DTO、PO、VO、枚举、服务契约及异常，不含 Controller/Mapper/实现 |
| `food-platform` | 共享鉴权、Session 主体、AppUserMapper、请求防滥用、Web/缓存配置及异常处理 |
| `food-user` | 账号、验证码、登录、资料审核及个人/管理接口 |
| `food-catalog` | 菜品、地区、标签、导入、公共发现缓存及城市坐标 |
| `food-achievement` | 成就与蚀刻章 |
| `food-community` | 收藏、心愿单、评论、点赞、打卡及主页聚合 |
| `food-media` | 图片存储、资产 Mapper、隔离解码、导出与维护任务；仅依赖 common/platform |
| `food-agent` | Java 侧 AI 接口与内部 Agent 接口；Python 工程仍为根目录 `agent-service` |
| `food-boot` | 兼容启动、历史迁移 SQL、原有集成测试及组装测试 |
| `food-user-client` | 内部账号状态和公开资料 HTTP 适配器，不包含用户业务实现 |
| `food-core-boot` / `food-media-boot` / `food-user-boot` / `food-community-boot` / `food-gateway` | 五个独立可执行 JAR |

图片服务的 JAR 不包含 user/catalog/community/achievement 的业务实现。图片导出通过 `FoodImageLookup` 调用核心 `/api/foods/{id}?recordVisit=false`，转发调用者的 Session Cookie，让核心执行原有菜品可见性检查；不是直接信任客户端用户 ID。兼容模式使用本地适配器，避免内部 HTTP。

用户 Mapper 暂留共享层，是现有账号撤销、上传归属与外键的一项过渡依赖。媒体仅独立拥有图片 HTTP 入口和处理进程，还未做到完全的数据自治。

## 构建与兼容启动

Java 21，在本目录运行：

```sh
mvn --batch-mode --no-transfer-progress clean verify
java -jar target/dayanfood-backend-0.0.1-SNAPSHOT.jar
```

`food-boot` 沿用后端根 `target`；普通模块及独立启动工程输出到各自 `target`。原来的生产/性能 Dockerfile 继续运行兼容 JAR。定向测试仍支持 `mvn -Dtest=FoodVisitControllerTests test`。

## 拆分启动

先配置已有 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`。另外设置随机的 `USER_SERVICE_TOKEN`（至少 32 字符，四个业务服务相同）。先启动 core 并等待 Flyway 完成，再在五个终端运行：

```sh
java -jar food-core-boot/target/food-core-boot-0.0.1-SNAPSHOT.jar
java -jar food-user-boot/target/food-user-boot-0.0.1-SNAPSHOT.jar
java -jar food-media-boot/target/food-media-boot-0.0.1-SNAPSHOT.jar
java -jar food-community-boot/target/food-community-boot-0.0.1-SNAPSHOT.jar
java -jar food-gateway/target/food-gateway-0.0.1-SNAPSHOT.jar
```

不要同时在 8080 启动兼容应用和 Gateway。核心、用户、社区与图片默认只监听 127.0.0.1；跨容器使用 `SERVER_ADDRESS=0.0.0.0`，并用网络隔离限制直连。`SERVER_PORT` 分别设置业务服务端口，`GATEWAY_PORT` 设置网关端口。图片服务与网关的 `CORE_SERVICE_URL` 默认 `http://127.0.0.1:8081`，网关 `MEDIA_SERVICE_URL` 默认 `http://127.0.0.1:8082`。核心、图片、社区和网关使用 `USER_SERVICE_URL`，默认 `http://127.0.0.1:8083`。网关的 `COMMUNITY_SERVICE_URL` 默认 `http://127.0.0.1:8084`。邮件和初始管理员配置属于用户服务。

`UPLOAD_DIRECTORY` 必须指向原有上传文件的绝对目录，仅媒体服务负责 `/uploads/**`。原目录不自动迁移，不要把服务拆分误当作更换空上传目录。

统一部署入口：复制 `../ops/services/.env.example` 为 `.env`，配置现有基础设施和上传目录，再从仓库根执行：

```sh
docker compose --env-file ops/services/.env -f ops/services/compose.yml config --quiet
docker compose --env-file ops/services/.env -f ops/services/compose.yml up -d --build
```

此 Compose 不创建或清空业务数据库，也不替代已有 MySQL/Redis。只发布本机 8080，公网由现有 HTTPS 入口转发。镜像 UID 10001 必须有原上传目录写权限。修改网络段时同步配置 `SERVICE_SUBNET` 与 `TRUSTED_PROXY_CIDRS`。此网关按公网边缘处理转发头；若前面另有代理，当前限流按该代理地址聚合，需要另外设计受信代理链，不能直接信任任意 `X-Forwarded-For`。

## 路由与鉴权

```text
web / App / 微信小程序
           │
      Gateway :8080
           ├─ 登录、账号、个人资料、成就 → user :8083
           ├─ 收藏、心愿单、评论、点赞、打卡、统计、公开主页 → community :8084
           ├─ /api/images/**、/uploads/** → media :8082
           └─ 其他 /api/**               → core :8081
core / media / community → user 内部账号状态查询
community 公开主页 → user 公开资料查询 + 菜品只读投影
media 图片导出 → core 菜品可见性查询
```

网关不改写 API 路径，不自动重试写请求；Session Cookie、CSRF 和预期用户 ID 原样透传。业务服务继续执行 Spring Security 和账号撤销校验。网关拒绝 `/api/internal/**`、歧义编码路径以及客户端伪造的身份/转发头。内部 Python Agent 应直连受限网络中的核心服务，并保留已有内部令牌与签名校验，不经过公网网关。

只有核心执行 Flyway。所有 31 份历史 SQL 仍位于 `food-boot/src/main/resources/db/migration`，core 构建时打入同一组资源，未修改历史内容。用户、媒体和社区不执行迁移。新增 SQL 继续添加下一版本。

共享默认配置在 `food-platform/src/main/resources/backend-defaults.yml`；启动工程的 `application.yml` 先导入默认值，再用后续 YAML 文档覆盖服务名/端口。Mapper 使用 `classpath*:mapper/*.xml` 加载各模块资源。

## 验证

`ops/services/verify_community.py` 提供五进程社区 HTTP 回归（仅接受隔离的本地测试库）。`food-boot` 保留原有回归；打包测试验证 Mapper、迁移、组件与真实图片解码子进程。`food-gateway` 测试真实 HTTP 路由、Cookie/CSRF 透传、身份头过滤与内部接口阻断。`food-media-boot` 测试远程图片查询的会话转发、可见性错误和上游失败。`food-user-client` 验证内部令牌、缺失账号、上游故障与拒绝重定向；Session 回归验证服务故障保留会话、停用/版本撤销以及账号切换。显式 MySQL/Redis 测试仍需隔离测试实例和原有启用变量。

Gateway 使用与 Boot 3.5 对应的 [Gateway 4.3](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/starter.html)，独立于 Spring MVC 业务进程。
