/*
 * Validation parity test against the conformance corpus.
 *
 * Per the brief §"Done criteria":
 *   - All 7 invalid cases produce a ValidationError whose `keyword` matches
 *     the case's expected-error.json.
 *   - All 15 valid cases produce a typed PoppyDocument.
 *
 * Pure JVM test — no Compose, no Robolectric. Runs as part of `./gradlew check`.
 */

package dev.poppy.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ValidateTest {

    @Test
    fun `every valid corpus case decodes to a PoppyDocument`() {
        val cases = CorpusLoader.loadValidCases()
        assertTrue(
            "Expected at least 15 valid corpus cases, found ${cases.size}",
            cases.size >= 15,
        )
        val failures = mutableListOf<String>()
        for (case in cases) {
            val result = Poppy.validate(case.documentJson)
            if (result !is ValidationResult.Ok) {
                val errs = (result as ValidationResult.Failure).errors
                failures += "${case.slug}: ${errs.joinToString { "${it.keyword}@${it.path}: ${it.message}" }}"
            }
        }
        if (failures.isNotEmpty()) {
            fail("Valid cases failed validation:\n${failures.joinToString("\n")}")
        }
    }

    @Test
    fun `every invalid corpus case produces its declared keyword`() {
        val cases = CorpusLoader.loadInvalidCases()
        assertTrue(
            "Expected at least 7 invalid corpus cases, found ${cases.size}",
            cases.size >= 7,
        )
        val mismatches = mutableListOf<String>()
        for (case in cases) {
            val result = Poppy.validate(case.documentJson)
            if (result !is ValidationResult.Failure) {
                mismatches += "${case.slug}: expected Failure, got Ok"
                continue
            }
            val keywords = result.errors.map { it.keyword }.toSet()
            val expected = case.expectedError.keyword
            if (expected !in keywords) {
                mismatches += "${case.slug}: expected keyword '$expected', got $keywords " +
                    "(messages: ${result.errors.joinToString { it.message }})"
            }
        }
        if (mismatches.isNotEmpty()) {
            fail("Invalid-case keyword mismatches:\n${mismatches.joinToString("\n")}")
        }
    }

    @Test
    fun `isValid returns true for valid documents`() {
        val cases = CorpusLoader.loadValidCases()
        for (case in cases) {
            assertTrue("isValid should accept ${case.slug}", Poppy.isValid(case.documentJson))
        }
    }

    @Test
    fun `isValid returns false for invalid documents`() {
        val cases = CorpusLoader.loadInvalidCases()
        for (case in cases) {
            assertEquals(
                "isValid should reject ${case.slug}",
                false,
                Poppy.isValid(case.documentJson),
            )
        }
    }
}
