// Deterministic HTML serializer for snapshot comparison.
//
// We don't trust raw HTML strings to compare equal across runs — attribute order,
// quoting style, and whitespace all vary depending on how the renderer built the
// tree. So we parse the input with jsdom, walk the DOM, and emit a canonical form:
//
//   - Tag names lowercase.
//   - Attributes sorted alphabetically, double-quoted, lowercase names.
//   - Class lists sorted alphabetically and de-duplicated.
//   - Void elements (img, br, input, ...) emitted without trailing slash and
//     without a closing tag (HTML5 spec, not XHTML).
//   - Text nodes preserved verbatim except that each non-text element starts on
//     its own line and is indented two spaces per depth — the snapshot is for
//     human review, not byte-perfect parity with the live DOM.
//
// jsdom is a devDep of this package; it's only loaded when normalizeHtml is
// called. Renderer packages that just want loadCases() don't pay for it as long
// as they don't import this module.

import { JSDOM } from "jsdom";

// HTML5 void elements. Order doesn't matter; Set lookup is O(1).
const VOID_ELEMENTS = new Set([
  "area",
  "base",
  "br",
  "col",
  "embed",
  "hr",
  "img",
  "input",
  "link",
  "meta",
  "param",
  "source",
  "track",
  "wbr",
]);

/** Normalize an HTML fragment for stable snapshot comparison. */
export function normalizeHtml(html: string): string {
  const dom = new JSDOM(`<!doctype html><body>${html}</body>`);
  const body = dom.window.document.body;
  const lines: string[] = [];
  for (const child of Array.from(body.childNodes)) {
    serialize(child, 0, lines);
  }
  return lines.join("\n").trim();
}

function serialize(node: Node, depth: number, out: string[]): void {
  if (node.nodeType === 3 /* Text */) {
    const text = (node.nodeValue ?? "").trim();
    if (text) out.push(`${indent(depth)}${escapeText(text)}`);
    return;
  }
  if (node.nodeType !== 1 /* Element */) return;

  const el = node as Element;
  const tag = el.tagName.toLowerCase();
  const attrs = formatAttrs(el);

  if (VOID_ELEMENTS.has(tag)) {
    out.push(`${indent(depth)}<${tag}${attrs}>`);
    return;
  }

  const childNodes = Array.from(el.childNodes).filter(
    (n) => n.nodeType !== 3 || (n.nodeValue ?? "").trim().length > 0,
  );

  if (childNodes.length === 0) {
    out.push(`${indent(depth)}<${tag}${attrs}></${tag}>`);
    return;
  }

  out.push(`${indent(depth)}<${tag}${attrs}>`);
  for (const child of childNodes) {
    serialize(child, depth + 1, out);
  }
  out.push(`${indent(depth)}</${tag}>`);
}

function formatAttrs(el: Element): string {
  const pairs: { name: string; value: string }[] = [];
  for (const attr of Array.from(el.attributes)) {
    const name = attr.name.toLowerCase();
    let value = attr.value;
    if (name === "class") {
      const classes = Array.from(new Set(value.split(/\s+/).filter(Boolean))).sort();
      value = classes.join(" ");
    }
    pairs.push({ name, value });
  }
  pairs.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
  return pairs.map(({ name, value }) => ` ${name}="${escapeAttr(value)}"`).join("");
}

function indent(depth: number): string {
  return "  ".repeat(depth);
}

function escapeText(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function escapeAttr(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/"/g, "&quot;");
}
