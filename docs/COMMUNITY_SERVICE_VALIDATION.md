# 社区服务拆分验证

验证日期：2026-10-16。分支：`feat/service-split-mobile-clients`。使用 Java 21、隔离 MySQL 8.0.28（REPEATABLE READ）和 Redis 7.4；验证数据库与实际本地 `dayan_food_local` 分开。

## 构建与自动化回归

- 全 reactor `clean verify` 成功：151 个测试条目，140 个执行通过，11 个需显式启用真实 MySQL/Redis 的用例在此阶段跳过。
- 随后显式运行这 11 个用例，全部通过；包括并发幂等与删除后禁止重发、用户/菜品权限、事务锁、旧库归属升级、Redis 挑战原子兑换以及社区查询投影与原 catalog 查询结果对照。合计 151 个不同测试执行通过。
- Windows 缺少创建符号链接权限，本机命令明确排除既有 `ImageExportBoundsTests#escapedSymlinkRejected`；未宣称本机验证此用例，Linux CI 的原全量命令仍保留它。
- 核心 JAR 不包含社区、用户、成就、媒体业务实现；社区 JAR 包含 community 和 user-client，不包含 catalog/user/achievement/media/agent 业务实现。
- 兼容单体 JAR 实际启动，并通过菜品目录、地区、未登录 me 和 CSRF 共 4 项 HTTP 检查；18 个 Mapper XML 与 31 份迁移均正确加载，未改历史 SQL。
- 网关回归覆盖 18 个社区路径 × GET/POST/PATCH/DELETE 路由矩阵、混合前缀保留、Cookie/CSRF/预期身份透传和内部接口阻断。
- `docker compose --env-file ops/services/.env.example -f ops/services/compose.yml config --quiet` 通过。未构建或发布生产 Docker 镜像。

## 五进程真实接口

可复用脚本：`ops/services/verify_community.py`。启动 core/user/media/community/gateway 五个实际构建的 JAR，通过网关执行 **106 项 HTTP 检查**，包括：

- 本地注册、登录、跨服务 Session；收藏添加、重复添加、列表/分页、状态及删除。
- 心愿单直接关联、模糊匹配、重复提交和越权删除；核心新增菜品后，社区缓存立即使用新的发现版本。
- 点赞幂等、取消和匿名状态；评论创建、列表和分页；未审核菜品不能被收藏、点赞、评论、打卡或加入直接关联心愿单。
- 两个并发相同幂等键的公开打卡只得到同一结果、只产生一条关联评论；更改请求体返回 409。
- 打卡版本冲突回滚关联评论修改；公开转私密撤回评论，再转公开恢复；过期版本删除返回 409，正确版本删除同步撤回评论，旧幂等键重发返回 410。
- 他人不能查看、修改、删除个人打卡；无 CSRF 返回 403，预期账号不符返回 409。
- 原个人菜品、足迹、成就、刻蚀与公开主页仍可访问；直接请求错误服务返回 404。
- 用户停用后，社区请求拒绝原 Session；用户服务停机返回 503，恢复后原有效 Session 可继续使用，匿名评论不受影响。
- 社区独立重启不影响核心和用户入口，恢复后登录仍有效；核心停机后，社区评论与个人统计仍可读取（数据库保持可用）。

脚本中所有测试进程已停止，隔离验证容器和网络已清理。测试产物和详细日志保存在本机验证目录，不纳入仓库。

## 本地开发环境

启动脚本已扩展为五个 Java 服务，社区端口为 `18084`。复用原 MySQL/Redis 数据卷、上传目录及 `.local` 凭据；Web 与客户端仍使用原网关入口。注册邮箱验证的临时关闭仅在用户服务的 local profile 生效。

五个 Java 进程及 Vite 已重新启动并保留运行，网页地址仍为 `http://localhost:5173`。通过网页代理追加执行 15 项真实 HTTP 检查，覆盖免邮箱验证码注册、显式登录、核心个人菜品与社区收藏分页、退出及临时账号清理，全部通过。

架构限制和发布顺序见 [社区服务边界](COMMUNITY_SERVICE_BOUNDARIES.md)。本轮未拆分数据库、未改变 Session 格式、未修改 Web/App/小程序接口调用。
