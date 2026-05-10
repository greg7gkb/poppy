Image with required `url` and `alt` only. No explicit dimensions — renderers should use intrinsic image size or layout container width.

## Behavioral invariants

Each renderer's tests must verify:

- A platform-native image widget renders.
- The `url` is passed to the platform's image loader (or the host-provided loader on Android via `PoppyImageLoader`). The renderer never fetches the bytes itself.
- The `alt` value is exposed to the platform's accessibility tree (`<img alt>`, `Modifier.semantics { contentDescription = alt }` on Compose, `.accessibilityLabel(alt)` on SwiftUI).
- No explicit width or height attribute is applied (the image takes intrinsic or container size).
