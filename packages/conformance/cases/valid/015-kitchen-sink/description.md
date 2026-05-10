A realistic profile-card layout combining all v0.1 components: nested stacks (vertical containing horizontal containing vertical), an image, multiple Texts with varying tokens, and two buttons with navigate actions. Catches regressions in component interaction and exercises every component at least once.

## Behavioral invariants

This is the integration case. Each renderer's tests must verify that all of the per-component invariants above hold simultaneously when components are composed:

- Every component in the tree renders without error.
- Nested Stack axis layouts work correctly (outer vertical → middle horizontal → inner vertical).
- The Image at the avatar position renders at 48×48 with `cover` fit and the `alt` text exposed to a11y.
- All Text nodes apply their respective tokens (e.g. "Greg" is bold and `lg`; "Signed in" is `sm` and `secondary`-color; "You have 3 unread notifications." uses the `primary` color).
- Stack alignment tokens are honored: the avatar/details row uses `alignment: center`; the buttons row uses `alignment: end`.
- Both buttons dispatch their respective navigate actions verbatim on click. Clicking "Dismiss" yields `{ "type": "navigate", "uri": "poppy://notifications/dismiss-all" }`; clicking "View all" yields `{ "type": "navigate", "uri": "poppy://notifications" }`.
- The root Stack's `id: "screen-root"` is exposed as a stable identifier (HTML `id` attribute on web; tag/test-tag on mobile if the platform supports it).
