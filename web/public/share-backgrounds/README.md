# 分享卡片背景

美术背景图放置于此目录，命名为 `default.jpg`，建议使用至少 1600 × 960 的横向图片。
图片不存在时使用与网站一致的内置米白纸色渐变。用户也可在分享弹窗中导入 JPG、PNG 或 WebP（最多 10 MB）；导入图片仅用于当前卡片，不上传到服务器。

二维码使用当前网站域名及 Vue Router 生成的菜品地址。部署后请在正式域名下生成卡片，本机 localhost 地址无法供其他设备访问。

## 开发验收

先在 web 目录运行 npm run dev，再在另一个终端运行 npm run test:share。
脚本使用模拟 API 数据，不读取真实账户。默认使用已安装的 Chrome；可用 SHARE_TEST_BROWSER=msedge 切换 Edge。
SHARE_TEST_URL 可指定本地前端地址，SHARE_TEST_OUT 可指定截图与导出图片目录（默认系统临时目录下 dayan-share-card-test）。

检查范围：桌面和手机弹窗、真实 PNG 导出及二维码解码、统计数大于 50、单枚蚀刻章展示（优先已选中的蚀刻章）、
本地背景导入和恢复、无效图片、资料加载失败及重试、访客、长文本、缺失图片、中英文及 Escape 关闭。

统计接口为 GET /api/profile/stats，仅返回当前登录者的数据。
viewedFoodCount 为历史浏览过的公开菜品去重数；favoriteCount 为当前收藏的公开菜品数。
无需修改数据库结构。此接口需随前端一同部署；默认背景图更新后需重新构建前端。

## 正式环境导出排查

个人统计、公开资料、成就和自制蚀刻章独立加载。某项失败时保留成功内容，统计缺失显示“—”，并提示重试；不阻止已就绪卡片导出。恢复背景不会清除资料加载提示。

若正式环境统计持续缺失，请检查 /api/profile/stats 是否返回有效 JSON（viewedFoodCount、favoriteCount 均为非负整数）。404 可能意味着新版后端未部署；200 却返回 HTML 时需检查反向代理是否误将 API 回退到前端入口；500 则需查看后端日志。

可在运行 npm run build 后启动 npm run preview -- --port 5174，将 SHARE_TEST_URL 设为 http://127.0.0.1:5174 后运行 npm run test:share，以生产静态资源验证导出。测试涵盖接口 404、500、错误 HTML、全部个人资料不可用，以及降级卡片的实际 PNG 下载和二维码解码。
