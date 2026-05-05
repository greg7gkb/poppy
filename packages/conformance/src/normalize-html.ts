// HTML snapshot normalization for the conformance corpus.
//
// Phase 1 stub — full implementation lands in Week 3 alongside @poppy/client-web.
// The Week 3 implementation will:
//   - Parse with DOMParser (jsdom in tests).
//   - Walk the tree and emit elements with attributes sorted alphabetically.
//   - De-duplicate and alphabetize class names.
//   - Use HTML-spec self-closing rules (no trailing slash on void elements).
//   - Normalize entity encoding.
//   - Collapse insignificant whitespace between block elements.

/**
 * Normalize an HTML string for stable snapshot comparison.
 *
 * Phase 1 stub: trims whitespace only.
 */
export function normalizeHtml(html: string): string {
  return html.trim();
}
