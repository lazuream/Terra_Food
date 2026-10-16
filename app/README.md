# 手机 App 工程

使用 uni-app、Vue 3、TypeScript，面向 Android / iOS。共享页面与业务代码在 `../packages/mobile`，与现有 `web` 独立。不提供 H5 构建目标。

在仓库根目录使用 Node.js 22.14+（建议 24）、pnpm 11.25.0：

```sh
pnpm install --frozen-lockfile
pnpm --filter @terra/app build:demo
```

产物为 `app/dist/build/app`，可在 HBuilderX 中导入运行。示例模式显示示例标识，不开放登录和写入。

连接真实服务：复制 `.env.example` 为 `.env.local`，填写 `VITE_API_BASE_URL=https://你的网关域名/api`、`VITE_DEMO_MODE=false`，执行 `pnpm build:app`。真机不能使用电脑的 `localhost`。

这是 **App 源码和编译资源包**，不是 APK / IPA。首次打包需在 `src/manifest.json` 配置自己的 DCloud AppID，并在 HBuilderX 配置 Android 包名/签名，或 iOS Bundle ID、证书和描述文件。不要把签名私钥、证书密码写入 Git。当前未做真机、商店发布或支付/推送集成。

功能、目录和联调说明见 [移动端说明](../docs/mobile-clients.md)。
