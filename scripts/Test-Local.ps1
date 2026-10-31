param([switch]$SkipDevice)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $workspace
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080'
$env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
$env:FIREBASE_STORAGE_EMULATOR_HOST = '127.0.0.1:9199'
foreach ($port in @(9099, 8080, 9199, 5001)) {
    if (-not (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)) {
        throw "Local backend port $port is unavailable. Run scripts/Start-LocalBackend.ps1 first."
    }
}
& "$PSScriptRoot/Seed-LocalBackend.ps1"
if ($LASTEXITCODE -ne 0) { throw 'Local seed failed.' }
Set-Location -LiteralPath $workspace
Push-Location (Join-Path $workspace 'backend')
try {
    & node --test --test-concurrency=1 test/domain.test.js test/integration.test.js
    if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' }
} finally { Pop-Location }
$tasks = @(':app:assembleDebug', ':core:model:test', ':app:testDebugUnitTest', ':feature:community:testDebugUnitTest', ':app:lintDebug', ':feature:community:lintDebug')
if (-not $SkipDevice) {
    $adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
    $devices = & $adb devices
    if (-not ($devices -match '^emulator-\d+\s+device')) { throw 'Start an Android Studio emulator first, or use -SkipDevice for non-device checks.' }
    $tasks += ':app:connectedDebugAndroidTest'
}
& (Join-Path $workspace 'gradlew.bat') @tasks '--max-workers=1' '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' '-Pkotlin.compiler.execution.strategy=in-process' '--console=plain'
if ($LASTEXITCODE -ne 0) { throw 'Android verification failed. Inspect the build reports.' }
Write-Host 'Requested checks passed. This verifies implemented local features, not unimplemented or live services.'
