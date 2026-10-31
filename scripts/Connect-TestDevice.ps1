param([string]$Serial, [switch]$Install)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
foreach ($port in @(9099, 8080, 5001, 9199)) {
    if (-not (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)) {
        throw 'Start scripts/Start-LocalBackend.ps1 in another terminal and keep it running first.'
    }
}
$deviceArgs = @()
if ($Serial) { $deviceArgs = @('-s', $Serial) }
& $adb @deviceArgs get-state
if ($LASTEXITCODE -ne 0) { throw 'Connect and authorize one USB debugging device, or supply -Serial from adb devices.' }
foreach ($port in @(9099, 8080, 5001, 9199)) {
    & $adb @deviceArgs reverse "tcp:$port" "tcp:$port"
    if ($LASTEXITCODE -ne 0) { throw "Could not connect backend port $port." }
}
& "$PSScriptRoot/Seed-LocalBackend.ps1"
if ($LASTEXITCODE -ne 0) { throw 'Could not prepare demo accounts.' }
if ($Install) {
    $apk = Join-Path $workspace 'dist/Calaf-local-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) { throw "APK missing: $apk" }
    & $adb @deviceArgs install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK installation failed.' }
    & $adb @deviceArgs shell am start -n com.calaf.app/.MainActivity
    if ($LASTEXITCODE -ne 0) { throw 'Could not open Calaf.' }
}
Write-Host 'Connected. Keep the USB cable attached and the local backend running. Repeat this script after reconnecting or rebooting your phone.'
