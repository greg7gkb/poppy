/*
 * Standalone Gradle build for `@poppy/client-android`.
 *
 * Per ADR-0002 and docs/phase-2-plan.md §"poppy-android" → "Tooling notes":
 * this build does NOT participate in any parent settings. Running `./gradlew`
 * from this directory works in isolation; the repo root has no Gradle config.
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

rootProject.name = "poppy-android"
