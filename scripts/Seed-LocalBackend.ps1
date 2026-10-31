$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath (Join-Path $workspace 'backend')
$env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080'
& node scripts/seed.mjs
if ($LASTEXITCODE -ne 0) { throw 'Local fixture setup failed.' }
