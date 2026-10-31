$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath (Join-Path $workspace 'backend')
if (-not (Test-Path -LiteralPath 'node_modules/.bin/firebase.cmd')) {
    & npm.cmd ci --no-fund --no-audit
    if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed.' }
}
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
# Export staging must stay outside the watched Functions source directory on Windows.
Set-Location -LiteralPath $workspace
$snapshot = Join-Path $workspace '.firebase/local-data'
$arguments = @('emulators:start', '--project', 'demo-calaf', '--config', 'firebase.json', '--only', 'auth,firestore,functions,storage', '--export-on-exit', $snapshot)
if (Test-Path -LiteralPath (Join-Path $snapshot 'firebase-export-metadata.json')) {
    $arguments += @('--import', $snapshot)
}
Write-Host 'Keep this terminal open while using Calaf. Press Ctrl+C once to save local accounts before stopping.'
& '.\backend\node_modules\.bin\firebase.cmd' @arguments
