/*
 * Poppy Android example application.
 *
 * Demonstrates how a host integrates `dev.poppy:client-android` as a Gradle
 * dependency. Min SDK 24 to match the library; produces a debuggable APK
 * via `./gradlew :app:assembleDebug`. Install on a connected device or
 * emulator with `./gradlew :app:installDebug`.
 */

plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "dev.poppy.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.poppy.example"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0-alpha"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
        }
    }

    // Match the library's exclude rules so duplicate META-INF entries
    // from transitive Compose deps don't break packaging.
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
            )
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // The library under demonstration. Resolved via composite build —
    // see settings.gradle.kts. The artifact name `poppy-android` matches
    // the included build's rootProject.name.
    implementation("dev.poppy:poppy-android:0.2.0-alpha")

    // Host-side Compose runtime. The library re-exports compose-ui and
    // foundation as `api`, but the app needs activity-compose itself
    // for `setContent`.
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
}
