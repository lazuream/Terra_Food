# 微信小程序工程

这是微信小程序工程，不是 H5 网站。使用 uni-app、Vue 3、TypeScript，与 `app` 共享 `../packages/mobile`，与现有 `web` 独立。

在仓库根目录使用 Node.js 22.14+（建议 24）、pnpm 11.25.0：

```sh
pnpm install --frozen-lockfile
pnpm --filter @terra/miniprogram build:demo
```

用微信开发者工具导入 `miniprogram/dist/build/mp-weixin`。未填写 AppID 时编译器生成游客项目，只用于本地示例预览。

真实联调：

1. 在 `src/manifest.json` 的 `mp-weixin.appid` 填入自己的微信小程序 AppID。
2. 复制 `.env.example` 为 `.env.local`，将 `VITE_API_BASE_URL` 设置为网关的 HTTPS 地址（以 `/api` 结尾），`VITE_DEMO_MODE=false`。
3. 在微信小程序后台配置对应的 request 合法域名；图片使用网关 `/uploads/` 或已配置的图片域名。
4. 执行 `pnpm build:mini`，重新导入编译目录进行开发者工具和真机联调。

现阶段使用原有用户名/邮箱和密码登录。微信 `wx.login` 的账号绑定、手机号授权尚未实现，不能把游客 AppID 用于发布。未上传或发布到微信平台。

功能、目录和联调说明见 [移动端说明](../docs/mobile-clients.md)。
