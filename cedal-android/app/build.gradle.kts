plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.xhacker.cedal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xhacker.cedalmobiledev"
        minSdk = 26
        targetSdk = 36
        // Was hardcoded at 1 forever - the update-gate check
        // (UpdateGateScreen.kt's UpdateCheckEffect) compares THIS real
        // installed versionCode against GET /app-version, so every build
        // reported itself as "always version 1" no matter what was
        // actually installed. The moment anything higher got published via
        // App Updates, every device - even ones on the very latest code -
        // was stuck seeing "update available" forever, since reinstalling
        // never changed this number. Must be bumped to keep pace with
        // whatever gets published (Admin > App Updates' Version Code
        // field) - found 2026-08-09 when a freshly-updated device still
        // showed the outdated prompt.
        versionCode = 5
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    // ProcessLifecycleOwner — used to detect the whole app going to the
    // background/foreground for the "lock on exit" security feature.
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.google.dagger:hilt-android:2.60")
    ksp("com.google.dagger:hilt-android-compiler:2.60")
    // Dagger's generated code (in this module) references these annotations
    // directly, but Dagger itself declares the dependency as compileOnly -
    // which Gradle never propagates transitively - so it must be added here too.
    compileOnly("com.google.errorprone:error_prone_annotations:2.50.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    // DocumentFile — real-folder (Storage Access Framework) backing for
    // Code > Documents, so files genuinely persist on the phone's storage.
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Local cache for friends/conversations - lets the Chats list and
    // friends list still show (read-only, last-known) data when there's no
    // internet, instead of just going blank. See data/local/.
    // 2.6.1 generates Java bridge code with wildcard-erasure bugs against
    // this project's Kotlin/KSP version (name-clash compile errors on any
    // suspend DAO method with a non-Unit return) - 2.8.4 is the fix.
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Loads real avatar/sticker photos from their uploaded URL - every
    // avatar in the app was a plain initial-letter circle before this,
    // there was no image-loading library at all.
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ✚ > QR tab - core is pure-Java (no Android deps), used only to encode
    // a BitMatrix we render into a Bitmap ourselves (see QrCode.kt). The
    // scanner side (CameraX live preview + ML Kit) reads a QR back off the
    // camera feed - first real in-app camera preview in this app; the
    // existing chat camera attachment just fires ACTION_IMAGE_CAPTURE.
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Push notifications (2026-08-13) - the app previously had zero push
    // infrastructure, everything was poll-while-alive (see
    // MessageNotificationSession's own doc comment) - meaning nothing ever
    // notified once Android killed the process. CedalMessagingService
    // receives the actual push; the BOM pins compatible versions instead
    // of hand-picking one.
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-messaging")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Adding the Firebase BOM (above) pulled in a transitive guava/errorprone
// graph whose runtime classpath resolves error_prone_annotations to an
// older version than Dagger's compileOnly declaration needs - AGP's
// consistent-resolution check then fails the whole build over the
// mismatch. Forcing one version everywhere is the standard fix.
configurations.all {
    resolutionStrategy {
        force("com.google.errorprone:error_prone_annotations:2.50.0")
    }
}
