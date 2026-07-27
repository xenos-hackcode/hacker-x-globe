plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
}

group = "com.xhacker.cedal"
version = "0.1.0"

application {
    mainClass.set("com.xhacker.cedal.ApplicationKt")
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.13"
val exposedVersion = "0.55.0"

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")

    // Outbound HTTP client — used by MarketDataService to fetch real crypto
    // prices from CoinGecko's public API.
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.h2database:h2:2.3.232")
    // Postgres/Cloud SQL - used instead of the local H2 file when DATABASE_URL is set (Cloud Run).
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.19.1")

    implementation("org.mindrot:jbcrypt:0.4")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Real signup/verification emails via plain SMTP (see EmailService) -
    // provider-agnostic (Gmail, Google Workspace, Microsoft 365, or any
    // company domain's mail server), configured entirely through
    // SMTP_HOST/PORT/USERNAME/PASSWORD/FROM env vars, not hardcoded to one
    // provider.
    implementation("com.sun.mail:jakarta.mail:2.0.1")

    // Avatar + sticker image uploads - stores objects in the project's
    // existing Firebase Storage bucket (see ImageUploadService), reusing
    // the Cloud Run service account's already-granted Editor role rather
    // than provisioning a new bucket or service account.
    implementation("com.google.cloud:google-cloud-storage:2.70.0")
    // Cloud Build + Cloud Run Admin have no simple Ktor-friendly client -
    // this is used only to fetch an OAuth access token from this
    // container's own Application Default Credentials (the identity Cloud
    // Run already runs it as, no separate key file), then those two APIs
    // are called as plain REST like everything else in this file (see
    // DeployService). Already a transitive dependency of google-cloud-
    // storage above in practice, but explicit here since this is now a
    // deliberate, direct use of it, not incidental.
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}
