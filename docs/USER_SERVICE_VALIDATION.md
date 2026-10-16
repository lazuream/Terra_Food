# 用户服务拆分验证（2026-10-16）

分支：`feat/service-split-mobile-clients`。本轮没有修改数据库历史迁移、外部 API URL、Session 主体序列化类或前端代码。

## 自动化回归

Java 21、Maven 3.9.14；全 reactor 15 个项目（含父 POM）构建成功。

```sh
mvn --batch-mode --no-transfer-progress '-Dtest=*Tests,!ImageExportBoundsTests#escapedSymlinkRejected' verify
```

- 常规回归与打包检查：147 项中执行 136 项，全部通过；11 项外部 MySQL/Redis 测试按原约定默认跳过。
- 另外在隔离 MySQL 8.0.28（REPEATABLE READ）与 Redis 7.4 上启用并执行全部 11 项，全部通过：`DayanFoodApplicationTests`、`SecurityBusinessMysqlTests`、`AtomicChallengeRedisTests`、`CollectionMysqlTests`、`LegacyOwnershipUpgradeMysqlTests`。
- 两组共覆盖 147 项不同测试，无失败。原有 `escapedSymlinkRejected` 因本机 Windows 创建符号链接权限限制未运行；未修改该测试，Linux CI 的常规 verify 仍会运行它。
- 新增/扩展覆盖：具体用户路由与混合前缀分流、内部接口阻断及令牌过滤、内部鉴权、远程账号查询失败/重定向/无效内容、账号撤销、服务中断保留会话、三个独立 JAR 的业务依赖隔离。

显式集成回归仍沿用现有 `RUN_AUDIT_BUSINESS_MYSQL_TESTS`、`RUN_MYSQL_FLOW_TESTS`、`AUDIT_REDIS_PORT`、`MIGRATION_DB_URL` 及 DB/Redis 环境变量。只能指向可丢弃的测试数据库。

## 四个真实进程的 HTTP 联调

以独立端口启动 gateway、core、user、media，连接隔离 MySQL/Redis，执行 45 项检查并全部通过：

- 原 URL 获取 CSRF、管理员登录、me、后台用户/菜品列表。
- 同一 Session 跨 user/core/media 访问个人菜品、足迹、成就、刻蚀、收藏、图片上传/导出和菜品创建。
- 公开用户主页通过内部查询聚合；identity 返回值仅包含四个规定字段。
- 网关阻断内部用户接口；直连内部接口缺少令牌返回 401。
- core 不再提供 auth/me，user 不包含菜品路由，media 不包含菜品目录路由，均按原异常结构返回 404。
- 身份切换保护返回 409；缺失 CSRF 的资料写入返回 403。
- 将测试账号停用并递增 auth_version 后，核心请求返回 401，原共享 Session 失效；重新激活后可重新登录。
- 资料更新、退出登录后图片接口拒绝旧会话。
- 停止用户进程后，核心受保护请求返回 503；匿名菜品浏览仍成功。用户进程恢复后，原登录会话继续可用。

另启动兼容单体 JAR，验证菜品目录、地区、未登录 me 与 CSRF 四个 HTTP 检查；全部通过。

## 配置与范围

- `docker compose --env-file ops/services/.env.example -f ops/services/compose.yml config --quiet` 通过，CI 增加相同结构检查（使用 Linux 上传目录）。
- 31 份历史 SQL 与分支 HEAD 的内容逐份比较一致。
- `git diff --check` 通过。
- 没有部署生产环境；Compose 验证为配置校验，四服务实际运行验证使用本机 Java 进程。
- 邮件验证码服务实现保持原样；没有向真实邮箱发送邮件，未验证真实 SMTP 投递。
- Web、App、小程序源码未变，本轮未重复构建移动端安装包。

测试结束后关闭临时应用进程与隔离数据库容器。数据边界及上线顺序见 [用户服务边界说明](USER_SERVICE_BOUNDARIES.md)。
