# 本地运行

当前采用 gateway + user + core + media 四个 Java 进程，以及 Vite 网页。MySQL/Redis 通过独立 Compose 项目 `terra-food-local` 运行，不占用其他项目的默认端口。

| 入口 | 地址 |
| --- | --- |
| 网页 | http://localhost:5173 |
| 注册 | http://localhost:5173/register |
| API 网关 | http://127.0.0.1:18080/api |
| 核心 / 图片 / 用户 | 127.0.0.1:18081 / 18082 / 18083 |
| MySQL / Redis | 127.0.0.1:13306 / 16379 |

## 启停

前提：Docker Desktop 已启动，Java 21、Python 3、Node.js 22.14+；后端 JAR 已构建，web/node_modules 已安装。原有 Maven 配置若依赖企业镜像，需要指定可用的 settings.xml。本机已经完成依赖安装与构建。重新修改后端模块后使用 `mvn clean package`，避免增量打包把旧模块留在启动 JAR；启动器会检查嵌套模块是否与当前产物一致。

```powershell
# 从仓库根启动，后台隐藏窗口。已有 .local 配置会复用。
./ops/local/start.ps1 -Python 'D:/work/software/anaconda/python.exe'

# 停止这组本地进程和容器，保留数据库/Redis 卷
./ops/local/stop.ps1
```

首次启动默认使用 mysql:8.4。本机已复用缓存的 MySQL 8.0.28 镜像，镜像选择保存在 `.local/environment.json`。不要在同一数据卷上随意切换 MySQL 主版本。首次部署也可通过 `-MysqlImage` 指定镜像。

运行状态见 `.local/runtime.json`；启动进度见 `.local/launcher.log`、错误见 `.local/launcher-error.log`，各进程输出分别存入 `.local/*.log`。本地管理员账号在 `.local/admin.txt`，数据库凭据在 `.local/environment.json`，这些文件均被 Git 忽略。

这是独立的 `dayan_food_local` 数据库，首次启动执行现有 31 份迁移和初始化数据，不读取或覆盖其他项目数据库。上传目录使用原 `dayanfood-backend/uploads`。

## 暂时关闭注册邮箱验证

启动器仅对用户服务设置：

```text
SPRING_PROFILES_ACTIVE=local
REGISTRATION_EMAIL_VERIFICATION_ENABLED=false
```

`web/.env.local` 配置：

```text
VITE_BACKEND_TARGET=http://127.0.0.1:18080
VITE_REGISTRATION_EMAIL_VERIFICATION=false
```

保留邮箱格式、唯一性、密码要求、注册限流及 CSRF；仅跳过注册邮箱挑战，不发注册邮件。密码找回仍需原邮箱验证。后台开关默认 true，关闭时必须有 local profile 且不能同时有 prod；网页生产构建始终展示验证码。

恢复时删除启动器中 `REGISTRATION_EMAIL_VERIFICATION_ENABLED=false` 的覆盖（或改为 true），将网页变量设为 true，并重启本地服务。真实邮件验证还需要配置 SMTP。

## App 工程

`app/.env.local` 已设置真实 API，`VITE_DEMO_MODE=false`；本次资源输出到 `app/dist/build/app`，不包含 APK/IPA，也未新增 H5 工程。

App 雏形仅支持已有账号登录：先在网页注册，再在 App 中使用该账号。

以后有设备时：

1. 在 HBuilderX 导入 `app` 工程，配置你自己的 DCloud AppID；当前 manifest 的 AppID 为空，不能当作已具备签名的安装包。
2. Android USB 调试可执行 `adb reverse tcp:18080 tcp:18080`，继续使用当前 `http://127.0.0.1:18080/api`。Android 模拟器可改为 `http://10.0.2.2:18080/api`。更换地址后重新编译。
3. 使用 Node 22.14+ 和 pnpm 11.25.0，在根目录执行 `pnpm --filter @terra/app dev` 持续生成调试资源，或 `pnpm build:app` 生成编译资源。编译器也支持通过 HBuilderX 导入 `app/dist/build/app` 运行。
4. 本地入口仅监听回环地址，未开放局域网、未调整防火墙。iOS 真机和无线连接需要另行配置可访问的开发地址。

未配置真实模型服务，因此 Python Agent/AI 对话没有随本次基础业务启动；需要另行配置模型与 Agent 凭据。

## 本次验证

2026-10-16：本地开关及原邮件验证码测试 4 项、网关路由测试 4 项通过；Web 类型检查/构建、App 类型检查/构建通过。真实接口完成 15 项注册、登录、跨服务个人数据、退出及临时账号清理检查。浏览器注册页已显示免验证码提示，未调用真实 SMTP。启动服务使用独立数据库，进程在后台保留运行。
