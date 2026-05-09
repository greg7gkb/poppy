// Render every valid corpus case and assert the normalized HTML matches the
// committed snapshot.web.html. To regenerate snapshots:
//   pnpm --filter @poppy/client-web snapshots:update
// PRs that change snapshots must justify each diff line-by-line — the snapshot
// is the cross-platform contract that Phase 2 renderers must honor.

import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { loadCases, normalizeHtml } from "@poppy/conformance";
import { beforeEach, describe, expect, it } from "vitest";
import { render } from "../src/index.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const corpusDir = join(__dirname, "..", "..", "conformance");
const { valid } = loadCases(corpusDir);

describe("@poppy/client-web — conformance corpus", () => {
  let container: HTMLElement;

  beforeEach(() => {
    document.body.replaceChildren();
    container = document.createElement("div");
    document.body.appendChild(container);
  });

  for (const c of valid) {
    it(`${c.name} renders to its committed snapshot`, () => {
      if (c.webSnapshot === undefined) {
        throw new Error(`${c.name} is missing snapshot.web.html`);
      }
      render(c.document, container, { onAction: () => {} });
      const actual = normalizeHtml(container.innerHTML);
      const expected = normalizeHtml(c.webSnapshot);
      expect(actual).toBe(expected);
    });
  }
});
