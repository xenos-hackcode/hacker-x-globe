plugins {
    id("com.android.application") version "9.0.1" apply false
    kotlin("android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    kotlin("plugin.serialization") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("com.google.dagger.hilt.android") version "2.60" apply false
    // Push notifications (2026-08-13) - reads app/google-services.json,
    // picking the client entry matching applicationId at build time (the
    // file already has both com.xhacker.cedalmobile and this dev build's
    // com.xhacker.cedalmobiledev as separate client entries).
    id("com.google.gms.google-services") version "4.4.2" apply false
}
