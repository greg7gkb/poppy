// Generates `snapshot.web.html` for every valid corpus case by running render()
// against a fresh jsdom and writing the normalized output back into the case dir.
//
// Run with: pnpm --filter @poppy/client-web snapshots:update
//
// Snapshot diffs are reviewed line-by-line in PRs (CONTRIBUTING). "I just
// regenerated the snapshots" is not a valid PR description.

import { writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { loadCases, normalizeHtml } from "@poppy/conformance";
import { JSDOM } from "jsdom";
import { render } from "../src/index.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const corpusDir = join(__dirname, "..", "..", "conformance");

const { valid } = loadCases(corpusDir);

let updated = 0;
for (const c of valid) {
  const dom = new JSDOM("<!doctype html><html><body><div id='app'></div></body></html>");
  const container = dom.window.document.getElementById("app") as HTMLElement;

  // The renderer reads several globals (`document`, `HTMLElement`,
  // `AbortController`, ...). jsdom enforces realm identity on EventTarget
  // listeners, so the AbortController must also come from jsdom — Node's
  // global one will fail at addEventListener.
  const globals = ["document", "HTMLElement", "AbortController", "Event"] as const;
  const saved: Record<string, unknown> = {};
  for (const key of globals) {
    saved[key] = (globalThis as Record<string, unknown>)[key];
    (globalThis as Record<string, unknown>)[key] = (
      dom.window as unknown as Record<string, unknown>
    )[key];
  }

  try {
    render(c.document, container, { onAction: () => {} });
    const html = container.innerHTML;
    const normalized = `${normalizeHtml(html)}\n`;
    writeFileSync(join(c.dir, "snapshot.web.html"), normalized, "utf-8");
    updated += 1;
  } finally {
    for (const key of globals) {
      (globalThis as Record<string, unknown>)[key] = saved[key];
    }
  }
}

console.log(`Updated ${updated} snapshot(s).`);
