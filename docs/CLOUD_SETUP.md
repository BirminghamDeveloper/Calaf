# Setting up the cloud environment (once)

The default cloud environment cannot build this project: it ships JDK 21 (the Gradle daemon
needs 25) and blocks `dl.google.com`, where the Android SDK lives. Create a dedicated environment.

1. Go to claude.ai/code. Under the message box, open the first control (the environment selector).
2. Select **New environment** at the bottom of the menu.
3. **Name:** `calaf-android`
4. **Network access:** choose **Custom**. Tick **Also include default list of common package
   managers**. In **Allowed domains** add, one per line:
   ```
   dl.google.com
   api.adoptium.net
   api.foojay.io
   ```
   (Maven Google, Gradle, npm and GitHub are already in the default list.)
5. **Environment variables:**
   ```
   ANDROID_HOME=/opt/android-sdk
   ANDROID_SDK_ROOT=/opt/android-sdk
   JAVA_HOME=/usr/lib/jvm/temurin-25
   GRADLE_OPTS=-Xmx4g
   ```
6. **Setup script:** paste the whole of `scripts/setup-cloud.sh`.
7. Select **Add environment**.

The first session takes a few minutes while the script runs. After that the environment is
cached, so later sessions start with the JDK and SDK already installed. The cache rebuilds
roughly weekly or when you change the script or the allowed domains.

If the first session reports a download failure, read the host name in the error and add it
to **Allowed domains**, then start a new session.
