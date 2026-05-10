Text with the `primary` color token. Renderers must map this to the host theme's primary text color (defaulted in the renderer's stylesheet; overridable via CSS custom properties on web).

## Behavioral invariants

Each renderer's tests must verify:

- The text content equals `"Primary color text"`.
- The applied color resolves through the host's theme's `primary` slot — **not** a hardcoded hex value. Tests should query the theme, not assert literal RGB.
