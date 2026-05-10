/*
 * Test PoppyHost that captures dispatched actions for assertion.
 *
 * Used by both BehaviorTest and UpdateSnapshots; the snapshot generator
 * doesn't care about actions but it needs a valid PoppyHost to render. It
 * also overrides isUrlAllowed to accept any URL so the corpus's
 * https://example.com URLs render in tests without needing the network layer
 * (Coil's loader returns a placeholder when offline; that's fine for
 * semantics-tree assertions).
 */

package dev.poppy.android

class TestHost : PoppyHost {
    val actions = mutableListOf<Action>()
    val errors = mutableListOf<Throwable>()

    override fun onAction(action: Action) {
        actions.add(action)
    }

    override fun onError(throwable: Throwable) {
        errors.add(throwable)
    }

    override fun isUrlAllowed(url: String, context: ImageContext): Boolean = true
}
