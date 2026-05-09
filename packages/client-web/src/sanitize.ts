// Sanitization for Poppy's web client.
//
// Two surfaces need defense:
//   1. Image `url`: must be a renderable image source. Reject `javascript:`,
//      `vbscript:`, and any non-`image/*` `data:` URI.
//   2. Action `uri`: never written into the DOM. Only forwarded to host.onAction.
//      The host owns whether to navigate, so XSS via uri is a host concern, not ours.
//
// Text content is always set via `textContent`, never `innerHTML` — see render.ts.

const UNSAFE_PROTOCOL = /^\s*(javascript|vbscript|file):/i;
const HTTP_PROTOCOL = /^\s*https?:/i;
const DATA_IMAGE = /^\s*data:image\/(png|jpeg|gif|webp|svg\+xml|avif)[;,]/i;

/**
 * Default allow-rule for `<img>` URLs:
 *   - http: / https:                 — allowed
 *   - relative URLs (no scheme)      — allowed
 *   - data:image/{png,jpeg,...}      — allowed
 *   - everything else (incl. data:text/html, javascript:, vbscript:) — rejected
 */
export function isUrlAllowedDefault(url: string, _context: "image"): boolean {
  if (UNSAFE_PROTOCOL.test(url)) return false;
  if (HTTP_PROTOCOL.test(url)) return true;
  if (DATA_IMAGE.test(url)) return true;
  // Relative URL: no scheme, not protocol-relative.
  if (!/^\s*[a-z][a-z0-9+.-]*:/i.test(url) && !url.startsWith("//")) return true;
  return false;
}
