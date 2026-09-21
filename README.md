# Ledger

一个本地优先的 Android 记账 App，支持自动记账、钱迹数据导入、数据库换机迁移。

## 技术栈

- Kotlin + Jetpack Compose
- Room (SQLite)
- Navigation Compose
- kotlinx.serialization

## 构建

```bash
./gradlew assembleDebug
## 更新与数据保留

- App 内置 GitHub Releases 更新检查，可下载 APK 并覆盖安装。
- 覆盖安装不会清除 Room 数据库、SharedPreferences 或本地文件。
- 数据库已移除破坏性迁移，并补齐 `1 -> 2` 显式迁移。
- 要保证 Android 允许覆盖安装，发布 APK 必须始终使用同一个签名证书。
- 签名与自动发布配置见 `docs/UPDATE_AND_SIGNING.md`。
## 性能版本

GitHub Actions 会生成 `Ledger-Performance-APK`。该版本关闭 debuggable、启用 R8 和资源压缩，适合日常安装体验。不要再使用未优化的 `app-debug.apk` 评价流畅度。