/*
 * Standalone Gradle build for the Poppy Android example app.
 *
 * Uses a Gradle composite build (`includeBuild`) to depend on the local
 * `packages/client-android/` library by Maven coordinate
 * `dev.poppy:client-android:0.2.0-alpha`. No publish-to-mavenLocal step is
 * needed — Gradle resolves the dependency by walking the included build's
 * project group/version metadata.
 */

@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "poppy-android-example"

// Include the library as a composite build. Its build.gradle.kts declares
// `group = "dev.poppy"` and `version = "0.2.0-alpha"`, and its rootProject
// name is `poppy-android` — combined, that's the
// `dev.poppy:poppy-android:0.2.0-alpha` Maven coordinate the app declares.
includeBuild("../../packages/client-android")

include(":app")
