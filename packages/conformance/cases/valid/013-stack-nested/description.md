A vertical stack with a nested horizontal stack between two texts. Verifies recursive structure works correctly across renderers and that each axis governs its own children's layout.

## Behavioral invariants

Each renderer's tests must verify:

- The outer Stack renders as a column container; the inner Stack renders as a row container nested inside it.
- Each Stack's `axis` governs only its own children — the outer stack's vertical axis does not flatten the inner stack's horizontal layout.
- All four Text descendants render in document order: "Header" above, then ["Left", "Right"] side-by-side, then "Footer" below.
