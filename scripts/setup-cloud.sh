#!/usr/bin/env bash
# Calaf cloud environment setup (Claude Code cloud sessions, Ubuntu 24.04, runs as root).
# Paste this whole file into the environment's "Setup script" field.
# Needs Custom network access: default package list PLUS dl.google.com, api.adoptium.net, api.foojay.io
# Idempotent: safe to run again inside a session with `bash scripts/setup-cloud.sh`.
set -euo pipefail

SDK=/opt/android-sdk
JDK=/usr/lib/jvm/temurin-25

# 1. JDK 25 (the Gradle daemon toolchain in gradle/gradle-daemon-jvm.properties needs 25; image ships 21)
install_jdk() {
  if [ -x "$JDK/bin/java" ]; then return; fi
  mkdir -p "$JDK"
  curl -fsSL "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    | tar -xz -C "$JDK" --strip-components=1
}

# 2. Android SDK command line tools, platform 37, platform tools
install_sdk() {
  if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    mkdir -p "$SDK/cmdline-tools"
    tmp=$(mktemp -d)
    # If Google publishes a newer build number, any recent commandlinetools zip works.
    curl -fsSL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
    unzip -q "$tmp/tools.zip" -d "$tmp"
    mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
    rm -rf "$tmp"
  fi
  yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
  "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platform-tools" "platforms;android-37.0" >/dev/null
}

command -v unzip >/dev/null || (apt-get update -qq && apt-get install -y -qq unzip)
install_jdk &
install_sdk &
wait

# 3. Tell Gradle where things are, and never try to download a JDK itself
mkdir -p /root/.gradle
cat > /root/.gradle/gradle.properties <<EOF
org.gradle.java.installations.paths=$JDK
org.gradle.java.installations.auto-download=false
org.gradle.daemon=true
# Maven Central often answers the cloud proxy with 429 Too Many Requests on a cold cache.
# Fewer parallel downloads and more patient retries avoid most of those failures.
org.gradle.parallel=false
org.gradle.workers.max=2
org.gradle.internal.repository.max.retries=8
org.gradle.internal.repository.initial.backoff=2000
org.gradle.internal.http.connectionTimeout=60000
org.gradle.internal.http.socketTimeout=60000
EOF

cat > /etc/profile.d/android.sh <<EOF
export ANDROID_HOME=$SDK
export ANDROID_SDK_ROOT=$SDK
export PATH=\$PATH:$SDK/platform-tools:$SDK/cmdline-tools/latest/bin
EOF

echo "Calaf setup complete: JDK $("$JDK/bin/java" -version 2>&1 | grep -m1 "version"), SDK at $SDK"
