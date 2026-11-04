#!/usr/bin/env bash
# Fails if the merged manifest of the demo flavour asks for the INTERNET permission or registers Firebase.
# Run after `./gradlew :app:processDemoDebugMainManifest` (assembleDemoDebug also does it).
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
manifest="$(find "$root/app/build/intermediates" -path '*demoDebug*' -name AndroidManifest.xml -path '*merged_manifest*' | head -1)"
if [ -z "$manifest" ]; then echo "No merged demoDebug manifest found. Build assembleDemoDebug first." >&2; exit 2; fi
echo "Checking $manifest"
if grep -q 'android.permission.INTERNET' "$manifest"; then echo "FAIL: demo manifest requests INTERNET" >&2; exit 1; fi
if grep -qi 'firebase' "$manifest"; then echo "FAIL: demo manifest mentions Firebase" >&2; exit 1; fi
echo "OK: demo flavour has no INTERNET permission and no Firebase components"
