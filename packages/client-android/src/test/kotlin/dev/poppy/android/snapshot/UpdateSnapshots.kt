/*
 * Snapshot regenerator. Walks the typed PoppyDocument tree of every valid
 * corpus case and rewrites snapshot.android.txt.
 *
 *   - Gated behind the env var `POPPY_UPDATE_SNAPSHOTS=1`.
 *   - When the env var is set, the test rewrites snapshot.android.txt for
 *     every valid case.
 *   - When the env var is NOT set, the test is a no-op so `./gradlew check`
 *     never accidentally rewrites committed snapshots.
 *
 * Invoke explicitly:
 *
 *     ./gradlew snapshotsUpdate
 *
 * (The Gradle task in build.gradle.kts sets the env var and filters to this
 * test class.)
 */

package dev.poppy.android.snapshot

import dev.poppy.android.CorpusLoader
import dev.poppy.android.Poppy
import dev.poppy.android.ValidationResult
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class UpdateSnapshots {

    @Test
    fun regenerateAllValidSnapshots() {
        val enabled = System.getenv("POPPY_UPDATE_SNAPSHOTS") == "1"
        assumeTrue(
            "POPPY_UPDATE_SNAPSHOTS not set — skipping snapshot regeneration. " +
                "Run `./gradlew snapshotsUpdate` to enable.",
            enabled,
        )

        val written = mutableListOf<String>()
        val failures = mutableListOf<String>()
        for (case in CorpusLoader.loadValidCases()) {
            try {
                val result = Poppy.validate(case.documentJson)
                require(result is ValidationResult.Ok) {
                    "${case.slug} did not validate"
                }
                val snapshot = SnapshotRenderer.render(result.document)
                val file = File(case.dir, "snapshot.android.txt")
                file.writeText(snapshot)
                written += "${case.slug} → ${file.relativeTo(File(".").canonicalFile)}"
            } catch (t: Throwable) {
                failures += "${case.slug}: ${t.message}"
            }
        }
        if (failures.isNotEmpty()) {
            fail("Failed to regenerate snapshots:\n${failures.joinToString("\n")}")
        }
        println("Wrote ${written.size} snapshot(s):")
        written.forEach { println("  $it") }
    }
}
