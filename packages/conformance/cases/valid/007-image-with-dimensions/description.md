Image with explicit width and height in logical pixels (CSS px / Android dp / iOS pt). Renderers must size to these values regardless of intrinsic image dimensions.

## Behavioral invariants

Each renderer's tests must verify:

- The image element exposes the declared width (200) and height (100) in logical pixels (CSS px / Android dp / iOS pt). Tests should assert the platform's logical size, not the underlying image bitmap dimensions.
- `alt` is still exposed to a11y per case 006.
