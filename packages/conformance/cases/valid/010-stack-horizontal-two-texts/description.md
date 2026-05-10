Horizontal stack with two Text children. Verifies `axis: horizontal` lays children out left-to-right (and that LTR is the default in v0.1).

## Behavioral invariants

Each renderer's tests must verify:

- A platform-native row container renders (`<div>` flex row on web, `Row` Composable on Compose, `HStack` on SwiftUI).
- Both Text children render in document order — the second appears to the right of the first on screen, in LTR locales (v0.1 does not specify RTL behavior).
