Stack with `padding: md` token. Renderers must apply a medium inset (16 logical px by default) around all children.

## Behavioral invariants

Each renderer's tests must verify:

- The padding around the stack's content resolves through the theme's `md` spacing token. Default mapping is 16 logical units (`padding: var(--poppy-space-md)` on web, `Modifier.padding(theme.spacing.md)` on Compose, `.padding(theme.spacing(.md))` on SwiftUI).
- No spacing between children is applied (only padding around them).
