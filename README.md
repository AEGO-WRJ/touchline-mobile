# 看球 · Touchline Android

独立安卓 App，Android 9 及以上。赛程、实时比分、关注球队、阵容站位与联赛榜单；不需要悬浮权限。

- 五大联赛、欧冠、美职联、中超及四项国内杯赛；按北京时间排列未来一周比赛，支持日期与赛事筛选。
- 关注球队可排序、持久保存及单独筛选。首页显示首位球队队徽。
- 比赛详情包含统计、关键事件、号码、位置、本场评分、首发与当前阵容；箭头标识换人，站位为阵型示意。
- 积分榜、射手榜、助攻榜保留赛季与并列排名，美职联分东区、西区和总榜。
- 前台赛中每 30 秒、其他时段每 2 分钟刷新，榜单每 5 分钟刷新；后台暂停，返回立即同步。

数据来自 ESPN 和 FotMob，可能延迟或缺失。缺失评分显示 `—`，轮次未提供时明确标注。设置和关注仅保存在本机；卸载会删除。

## 安装与构建

从 [Releases](https://github.com/AEGO-WRJ/touchline-mobile/releases) 下载 APK，允许本次安装后打开。后续版本使用同一签名，可直接覆盖升级。

Android Studio 打开仓库，或使用 JDK 17、Android SDK 35 和 Gradle 8.11.1 执行 `gradle :app:assembleDebug`。`web/` 为界面与数据处理，`app/` 为安卓宿主；不需要 Node 依赖。WebView 需保持更新。

发布通过 GitHub Actions 生成签名 APK；签名材料由仓库 Secrets 提供，不进入源码。原桌面版位于 [touchline-desktop](https://github.com/AEGO-WRJ/touchline-desktop)。
