Text with the `lg` size token. Verifies the size token is honored. Default mapping is 20 logical px (CSS px / Android sp / iOS pt).

## Behavioral invariants

Each renderer's tests must verify:

- The text content equals `"Large text"`.
- The applied font size resolves through the theme's `lg` size token — **not** a hardcoded value. Default mapping is 20 logical units; hosts may override.
