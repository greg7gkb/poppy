/*
 * Entry point for the Poppy Android example app.
 *
 * Lifecycle: this Activity owns one Composable (`PoppyDemoScreen`) which in
 * turn calls Poppy's library API. The real-world equivalent for a host app
 * is the same — drop a `Poppy(document, host)` Composable inside any
 * `setContent { ... }` block.
 */

package dev.poppy.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use the host's Material3 theme as the outer chrome. PoppyTheme
            // (set inside PoppyDemoScreen) is independent — it only governs
            // Poppy components, not the host's shell.
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PoppyDemoScreen()
                }
            }
        }
    }
}
