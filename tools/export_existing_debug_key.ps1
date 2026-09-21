param(
    [string]$KeystorePath = (Join-Path $env:USERPROFILE ".android\debug.keystore")
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $KeystorePath)) {
    throw "未找到 keystore：$KeystorePath`n请改为传入你当前安装版本所使用的签名文件。"
}

[Convert]::ToBase64String([IO.File]::ReadAllBytes($KeystorePath)) | Set-Clipboard

Write-Host "Keystore: $KeystorePath" -ForegroundColor Green
Write-Host "Base64 已复制到剪贴板。" -ForegroundColor Green
Write-Host ""
Write-Host "Android 默认 debug keystore 对应的 GitHub Secrets：" -ForegroundColor Yellow
Write-Host "LEDGER_KEYSTORE_PASSWORD=android"
Write-Host "LEDGER_KEY_ALIAS=androiddebugkey"
Write-Host "LEDGER_KEY_PASSWORD=android"
Write-Host "LEDGER_KEYSTORE_BASE64=<剪贴板内容>"
Write-Host ""
Write-Host "警告：私钥只能放进 GitHub Secrets，绝不能提交到仓库。" -ForegroundColor Red