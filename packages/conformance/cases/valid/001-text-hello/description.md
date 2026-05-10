Minimal Text component with the required `value` only. The smallest valid Poppy document — every renderer must support this.

## Behavioral invariants

Each renderer's tests must verify:

- The document validates and renders without error.
- A platform-native text widget is used (`<span>`, `Text` Composable, `Text` SwiftUI view).
- The rendered text content equals `"Hello"`.
- Default color, size, and weight tokens are applied (host theme defaults).
