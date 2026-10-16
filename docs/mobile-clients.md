# App 与微信小程序雏形

用户范围：仅 Android/iOS App 与微信小程序，不新增 H5 工程，不复用或改写现有 `web` 页面。

```text
app/                      uni-app 的 App 入口、manifest、页面注册和独立产物
miniprogram/              uni-app 的微信小程序入口、manifest、页面注册和独立产物
packages/mobile/src/     共享页面、双语文本、API 契约、会话客户端和样式
web/                      原有网站及管理后台
```

`app`、`miniprogram` 是独立工程，但不是两份重复业务实现。两个工程都经各自编译器输出；小程序中的自定义组件来自共享 Vue 源码，不加载网页。仓库根目录的 pnpm workspace 只管理移动端，不包含 `web`。

## 当前功能

- 寻味：首页、菜品分页、关键词搜索、地区筛选。
- 详情：图片、地区、故事、食材、地址、收藏与取消收藏。
- 登录：现有用户名/邮箱与密码，服务端 Session 和 CSRF 流程。
- 我的：会话恢复、收藏列表前 12 项、退出登录。
- 中英文切换、加载/空列表/网络失败状态。
- 显式示例模式：3 份示例食记；不会伪造登录成功或写入成功。默认真实模式，未配置 API 时显示连接配置提示。

此阶段未迁移地图、收录上传、评论、管理后台、音乐、分享图片、微信快捷登录。App 的签名安装包、小程序真机调试和平台发布需使用实际开发者配置完成。

## 运行

使用 Node.js 22.14+（建议 24）和 pnpm 11.25.0；系统旧 Node 14 不能运行当前工具链。

```sh
pnpm install --frozen-lockfile
pnpm typecheck
pnpm test:mobile
pnpm build:app
pnpm build:mini
```

只看界面时使用 `pnpm --filter @terra/app build:demo` 或 `pnpm --filter @terra/miniprogram build:demo`。正式接服务先分别创建 `.env.local`，配置 `VITE_API_BASE_URL` 和 `VITE_DEMO_MODE=false`。这些 `VITE_` 变量会编译进入客户端，不能放密钥。`manifest.json` 中的 AppID 属于公开标识，仍需替换为自己的应用。

构建结果分别位于 `app/dist/build/app`、`miniprogram/dist/build/mp-weixin`，不提交到 Git。App 构建输出需要 HBuilderX 继续运行/签名打包，不等同于 APK/IPA。

## 网络与登录

统一入口是 Gateway，所有业务路径沿用 `/api/**`，图片使用 `/uploads/**`。原来的单体启动模式也可以作为 API 地址。

原网站依赖浏览器 Cookie；App/小程序通过 `uni.request` 与网关通信，在平台适配层保存和回传 `SESSION`。只收集该网关的 Session Cookie，按 API 地址隔离缓存，不存密码。写请求先读取 `/auth/csrf`，登录后清除旧 CSRF；私有写入带 `X-Expected-User-Id`。旧会话的延迟响应不能覆盖新登录的凭据。退出须服务端确认，失败会提示重试。服务端鉴权、账号撤销检查与 CSRF 均保留。

真实环境需要 HTTPS 网关。微信开发者工具的域名调试开关不能替代正式合法域名配置。原有账号可以直接登录；微信身份换取、绑定旧账号与新用户注册流程留待下一阶段。

## 验证与边界

编译验证覆盖 App 资源与微信小程序模板/脚本，TypeScript 校验覆盖两端及共享源码。`packages/mobile/tests` 验证 Cookie、登录轮换、CSRF、身份固定、延迟响应和注销失败。浏览器中的隔离组件预览只能检查版式与交互，不能替代微信开发者工具、Android/iOS 真机验证。

构建依据：[uni-app CLI 文档](https://uniapp.dcloud.net.cn/quickstart-cli.html)、[官方 Vue 3 TypeScript 模板](https://github.com/dcloudio/uni-preset-vue/tree/vite-ts)。DCloud 相关编译包固定为同一版本，升级时需成组升级并重新检查两个目标。

当前 DCloud 插件的 peer 声明固定 Vite 5.2.8，本项目使用 Vite 5.4.21；安装会显示这一项版本警告，两种目标均已实际编译验证。未跳过类型检查或关闭小程序域名校验。真实 AppID 尚未配置，因此编译时会提示统计服务不可用；统计服务不是本雏形的运行依赖。
