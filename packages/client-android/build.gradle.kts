/*
 * `@poppy/client-android` — Kotlin + Jetpack Compose renderer for Poppy v0.1
 * documents. See docs/phase-2-plan.md §"poppy-android" for the design.
 *
 * Output: an AAR. Not published — consumers vendor or use as a project
 * dependency. See ADR-0005 for the dependency policy.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.poppy.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
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
        getByName("test") {
            kotlin.srcDirs("src/test/kotlin")
            // Make the conformance corpus available to JVM unit tests as a
            // resources root — `Class.getResource()` would only see things on
            // the test classpath, but we deliberately use direct File access
            // (see CorpusLoader.kt) so the corpus directory stays the API.
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Robolectric needs this to render Compose; see
            // https://robolectric.org/androidx_test/
            all { test ->
                test.systemProperty("robolectric.graphicsMode", "NATIVE")
            }
        }
    }

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
    jvmToolchain(libs.versions.java.get().toInt())
    compilerOptions {
        // Kotlin 2.0+ K2 compiler is the default; nothing to opt into.
        // We keep explicit API mode off for ergonomics; the public API surface
        // is small and audited via the README.
    }
}

dependencies {
    // --- Production deps ---
    api(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)

    // Coil image loading — bundled by default, swappable via
    // LocalPoppyImageLoader. See docs/phase-2-plan.md §"Image loading abstraction".
    implementation(libs.coil.compose)

    // Tooling for IDE previews (debug-only, not shipped in release AAR).
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // --- Test deps (JVM unit tests; run via `./gradlew check`) ---
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.activity.compose)
}

// --- Snapshot regeneration task ---
//
// Per docs/phase-2-plan.md §"poppy-android" → "Tooling notes" and brief
// §"Tests": snapshot regeneration is a deliberate developer act, NOT part of
// `check`. Invoke explicitly:
//
//     ./gradlew snapshotsUpdate
//
// The task runs only the `UpdateSnapshots` test class with the env var that
// gates the rewrite logic. Without the env var, that test is a no-op so it
// never accidentally rewrites snapshots during `check`.
tasks.register<Test>("snapshotsUpdate") {
    description = "Regenerates snapshot.android.txt for every valid corpus case."
    group = "verification"

    // Reuse the testDebugUnitTest classpath without coupling to the task graph
    // wiring. Robolectric + Compose UI test work the same way here.
    val unitTest = tasks.named("testDebugUnitTest", Test::class).get()
    testClassesDirs = unitTest.testClassesDirs
    classpath = unitTest.classpath

    useJUnit()
    filter {
        includeTestsMatching("dev.poppy.android.snapshot.UpdateSnapshots")
    }
    environment("POPPY_UPDATE_SNAPSHOTS", "1")
    outputs.upToDateWhen { false }
}
