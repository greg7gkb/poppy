Text with all optional fields populated (color + size + weight). Verifies the three tokens compose correctly without conflicting.

## Behavioral invariants

Each renderer's tests must verify:

- The text content equals `"All options"`.
- All three tokens are applied simultaneously to the same text node — color *and* size *and* weight, not just one or two.
- Token resolution still goes through the theme; tokens do not override each other.
