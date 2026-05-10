Button with a label and a navigate action. Pressing the button must dispatch the navigate action verbatim to the host's onAction callback. Renderers MUST NOT interpret the URI.

## Behavioral invariants

Each renderer's tests must verify:

- Renders as a platform-native button widget (`<button type="button">` on web, `Button(onClick = ...)` Composable on Compose, `Button(action: ...) { }` on SwiftUI).
- The `label` value `"Continue"` is the button's visible text and exposed to accessibility.
- A simulated click/tap fires `host.onAction(...)` exactly once with the action object equal (deep-equality) to `{ "type": "navigate", "uri": "poppy://continue" }`. The renderer must **not** parse, prefix, route, or otherwise transform the URI.
