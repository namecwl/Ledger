param(
    [string]$OutputPath = (Join-Path $PSScriptRoot "ledger-release.jks"),
    [string]$Alias = "ledger"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    throw "未找到 keytool。请先安装 JDK 17，或使用 Android Studio 自带的 keytool。"
}

$securePassword = Read-Host "请输入新的 release keystore 密码" -AsSecureString
$password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
)

& keytool -genkeypair -v `
    -keystore $OutputPath `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $password `
    -keypass $password `
    -dname "CN=Ledger, OU=Personal, O=Ledger, L=Shanghai, ST=Shanghai, C=CN"

[Convert]::ToBase64String([IO.File]::ReadAllBytes($OutputPath)) | Set-Clipboard

Write-Host ""
Write-Host "Keystore: $OutputPath" -ForegroundColor Green
Write-Host "Alias: $Alias" -ForegroundColor Green
Write-Host "Base64 已复制到剪贴板，请配置为 GitHub Secret：LEDGER_KEYSTORE_BASE64" -ForegroundColor Yellow
Write-Host "密码请分别配置为 LEDGER_KEYSTORE_PASSWORD 和 LEDGER_KEY_PASSWORD。" -ForegroundColor Yellow