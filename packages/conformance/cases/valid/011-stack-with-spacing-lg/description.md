Stack with `spacing: lg` token. Renderers must place a large gap (24 logical px by default) between children along the main axis.

## Behavioral invariants

Each renderer's tests must verify:

- The gap between children resolves through the theme's `lg` spacing token. Default mapping is 24 logical units (`gap: var(--poppy-space-lg)` on web, `Arrangement.spacedBy(theme.spacing.lg)` on Compose, `spacing: theme.spacing(.lg)` on SwiftUI).
- No padding is applied (only spacing).
