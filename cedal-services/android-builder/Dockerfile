FROM eclipse-temurin:17-jdk

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl unzip ca-certificates gnupg \
    && rm -rf /var/lib/apt/lists/*

# Node.js (for server.js)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# Gradle
ENV GRADLE_VERSION=8.7
RUN curl -fsSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -q /tmp/gradle.zip -d /opt \
    && rm /tmp/gradle.zip
ENV PATH="/opt/gradle-${GRADLE_VERSION}/bin:${PATH}"

# Android SDK cmdline-tools + platform + build-tools
ENV ANDROID_HOME=/opt/android-sdk
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL -o /tmp/cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true \
    && sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Pre-baked debug keystore at the standard AGP debug-signing location, so
# every build reuses the same key instead of racing to generate one.
RUN mkdir -p /root/.android \
    && keytool -genkeypair -v \
        -keystore /root/.android/debug.keystore \
        -storepass android -keypass android \
        -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"

WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev

COPY template/ /app/template/
# Warm the *global* Gradle dependency cache (downloaded jars, under
# ~/.gradle) so per-build compiles are fast - but NOT the template's own
# build output or its project-local .gradle/ incremental-compile cache.
# fs.cp() at runtime copies this whole directory into every fresh build's
# project dir - a baked-in build/.gradle here let Gradle conclude a fresh
# build was already up-to-date, silently keeping the template's ORIGINAL
# compiled MainActivity.class instead of ever compiling the user's actual
# code. Real bug, not a hypothetical: this shipped a real APK that crashed
# on launch with ClassNotFoundException for a class that was never compiled.
RUN gradle -p /app/template assembleDebug --no-daemon || true \
    && rm -rf /app/template/app/build /app/template/.gradle

COPY server.js ./

ENV PORT=8080
EXPOSE 8080
CMD ["node", "server.js"]
