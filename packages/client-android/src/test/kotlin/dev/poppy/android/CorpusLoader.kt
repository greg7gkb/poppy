/*
 * Conformance corpus loader for Android tests.
 *
 * Per docs/phase-2-plan.md §"poppy-android" → "Tooling notes": the corpus
 * directory is the API. We re-implement the loader (~30 LOC) rather than
 * porting `@poppy/conformance` to Kotlin. Cases live at
 * `../../packages/conformance/cases/{valid,invalid}/NNN-slug/`.
 *
 * The path is resolved relative to the Gradle module's working directory
 * (`packages/client-android/`), which is also what `./gradlew check` cwd's to.
 */

package dev.poppy.android

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExpectedError(
    val keyword: String,
    val path: String? = null,
)

data class ValidCase(
    val slug: String,
    val dir: File,
    val documentJson: String,
    val description: String,
    val webSnapshot: String?,
    val androidSnapshot: String?,
)

data class InvalidCase(
    val slug: String,
    val dir: File,
    val documentJson: String,
    val description: String,
    val expectedError: ExpectedError,
)

object CorpusLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** Repository's `packages/conformance/cases/` directory, resolved from CWD. */
    val casesDir: File by lazy {
        // The unit test working directory is `packages/client-android/`. Walk
        // up to `packages/`, then into `conformance/cases/`. We also tolerate
        // running from the repo root (e.g. from a fresh JVM).
        val candidates = listOf(
            File("../conformance/cases"),
            File("packages/conformance/cases"),
            File("../../packages/conformance/cases"),
        )
        candidates.firstOrNull { it.isDirectory }?.canonicalFile
            ?: error(
                "Conformance corpus not found. Looked in: ${candidates.joinToString { it.absolutePath }}",
            )
    }

    fun loadValidCases(): List<ValidCase> {
        val dir = File(casesDir, "valid")
        return dir.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name }
            ?.map { caseDir ->
                ValidCase(
                    slug = caseDir.name,
                    dir = caseDir,
                    documentJson = File(caseDir, "document.json").readText(),
                    description = File(caseDir, "description.md").readText(),
                    webSnapshot = File(caseDir, "snapshot.web.html").takeIf { it.exists() }?.readText(),
                    androidSnapshot = File(caseDir, "snapshot.android.txt").takeIf { it.exists() }
                        ?.readText(),
                )
            }
            ?: emptyList()
    }

    fun loadInvalidCases(): List<InvalidCase> {
        val dir = File(casesDir, "invalid")
        return dir.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name }
            ?.map { caseDir ->
                val expectedJson = File(caseDir, "expected-error.json").readText()
                InvalidCase(
                    slug = caseDir.name,
                    dir = caseDir,
                    documentJson = File(caseDir, "document.json").readText(),
                    description = File(caseDir, "description.md").readText(),
                    expectedError = json.decodeFromString(ExpectedError.serializer(), expectedJson),
                )
            }
            ?: emptyList()
    }
}
