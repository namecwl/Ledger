# 覆盖更新与签名配置

Android 只有在 **applicationId 相同** 且 **签名证书相同** 时，才允许新 APK 直接覆盖旧版本。覆盖安装不会卸载应用，因此 Room 数据库、SharedPreferences 和本地文件都会保留。

本项目只修改了版本号和界面，没有修改现有表结构，并已移除 `fallbackToDestructiveMigration()`。同时补上 `1 -> 2` 显式迁移，因此旧账本升级时不会清库。

## 第一次配置稳定签名

如果继续使用每次 GitHub Actions 自动生成的 debug 签名，不同构建之间证书可能变化，系统会拒绝覆盖安装。发布更新前建议配置固定 release 签名：

1. 在本机生成一个 release keystore，并妥善保存，不要丢失。
2. 将 keystore 文件转为 Base64。
3. 在 GitHub 仓库的 `Settings -> Secrets and variables -> Actions` 中添加：
   - `LEDGER_KEYSTORE_BASE64`
   - `LEDGER_KEYSTORE_PASSWORD`
   - `LEDGER_KEY_ALIAS`
   - `LEDGER_KEY_PASSWORD`
4. 推送 `v1.4.0` 形式的 tag，工作流会构建签名 release APK，并自动上传到 GitHub Release。

PowerShell 生成示例：

```powershell
keytool -genkeypair -v `
  -keystore ledger-release.jks `
  -alias ledger `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass "你的密码" -keypass "你的密码"

[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes("$PWD\ledger-release.jks")
) | Set-Clipboard
```

## 已经安装的旧版怎么办

- 如果旧 APK 与新 APK 使用同一签名，可以直接覆盖更新，数据保留。
- 如果旧 APK 是另一把签名生成的，第一次切换会被 Android 阻止。需要先用应用的数据库备份功能保存数据，再安装一次新签名版本并恢复。
- 从新签名版本之后，只要继续使用同一把 release key，后续更新都可以直接覆盖，不需要卸载。

## 应用内更新

设置页进入 `软件更新` 后会读取：

```text
https://api.github.com/repos/namecwl/Ledger/releases/latest
```

当最新 Release 包含 `.apk` 附件且版本号高于当前版本时，应用会下载 APK 并打开系统安装器。首次安装需要允许“安装未知应用”。
## 如果旧 APK 是本机 Android Studio 的 debug 包

默认 debug keystore 通常位于：

```text
C:\Users\你的用户名\.android\debug.keystore
```

如果旧 APK 就是本机调试构建，运行：

```powershell
.\tools\export_existing_debug_key.ps1
```

脚本会把 Base64 复制到剪贴板。GitHub Secrets 可使用：

```text
LEDGER_KEYSTORE_PASSWORD=android
LEDGER_KEY_ALIAS=androiddebugkey
LEDGER_KEY_PASSWORD=android
LEDGER_KEYSTORE_BASE64=剪贴板内容
```

这样首次 GitHub Release 也能尽量使用旧版相同签名。debug key 不适合公开项目长期使用，但在个人本地应用中可用于保留数据平滑升级。