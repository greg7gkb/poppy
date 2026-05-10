Vertical stack with two Text children. Verifies the basic stacked layout — children flow top to bottom on every renderer.

## Behavioral invariants

Each renderer's tests must verify:

- A platform-native column container renders (`<div>` flex column on web, `Column` Composable on Compose, `VStack` on SwiftUI).
- Both Text children render in document order — the second appears below the first on screen.
- No spacing or padding tokens are applied (this case is the baseline).
