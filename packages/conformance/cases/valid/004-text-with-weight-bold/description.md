Text with `bold` weight. Renderers must apply a heavy font weight (700 by default).

## Behavioral invariants

Each renderer's tests must verify:

- The text content equals `"Bold text"`.
- The applied font weight resolves through the theme's `bold` weight token. Default mapping is 700 (`FontWeight.Bold` on Compose, `.bold` on SwiftUI, `font-weight: 700` on web).
