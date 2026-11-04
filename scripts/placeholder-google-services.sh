#!/usr/bin/env bash
# Writes a PLACEHOLDER app/google-services.json so Gradle can build without real Firebase credentials.
# For CI and cloud sessions only. The file is git ignored. Never commit a real one.
# Does nothing if a file already exists (so a real local file is never overwritten).
set -euo pipefail
out="$(dirname "$0")/../app/google-services.json"
if [ -f "$out" ]; then echo "google-services.json already exists, leaving it alone"; exit 0; fi
cat > "$out" <<'JSON'
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "calaf-placeholder",
    "storage_bucket": "calaf-placeholder.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
        "android_client_info": { "package_name": "com.calaf.app" }
      },
      "api_key": [ { "current_key": "placeholder-not-a-real-key" } ]
    }
  ],
  "configuration_version": "1"
}
JSON
echo "Wrote placeholder $out (not a real Firebase config)"
