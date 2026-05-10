# Examples

Sample applications using Poppy. Each example is a self-contained, runnable project with its own README and build instructions.

| Example | Status | Run with |
|---|---|---|
| [`web/`](web/) | v0.1 shipped | `cd web && ./run_server.sh` (then open `http://localhost:8000/examples/web/`) |
| [`android/`](android/) | v0.2 shipped | `cd android && ./gradlew :app:installDebug` (or `:app:assembleDebug` for an APK) |
| [`ios/`](ios/) | v0.2 shipped | `cd ios/PoppyExample && swift run` (macOS); Xcode integration notes in the README |
| `server-node/` | not yet | — |

Each example is intentionally minimal: a smoke-test scope that demonstrates the integration loop on its platform (validate → host → theme → render) without an app shell beyond what's necessary to be runnable. Use them as a reference when wiring Poppy into your own app.
