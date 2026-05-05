// Runs the @poppy/conformance corpus through validate().
//
// Schema-level test coverage lives here (one test per case). API-level smoke
// tests for validate() / isValid() live in validate.spec.ts.

import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { loadCases } from "@poppy/conformance";
import { describe, expect, it } from "vitest";
import { validate } from "../src/index.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const corpusDir = resolve(__dirname, "..", "..", "conformance");
const { valid, invalid } = loadCases(corpusDir);

describe("conformance corpus — valid cases", () => {
  for (const c of valid) {
    it(`${c.name} validates`, () => {
      const result = validate(c.document);
      if (!result.ok) {
        console.error(c.name, result.errors);
      }
      expect(result.ok).toBe(true);
    });
  }
});

describe("conformance corpus — invalid cases", () => {
  for (const c of invalid) {
    it(`${c.name} fails with keyword '${c.expectedError.keyword}'`, () => {
      const result = validate(c.document);
      expect(result.ok).toBe(false);
      if (!result.ok) {
        const keywords = new Set(result.errors.map((e) => e.keyword));
        if (!keywords.has(c.expectedError.keyword)) {
          console.error(
            `${c.name}: expected '${c.expectedError.keyword}' in keywords, got [${[...keywords].join(", ")}]`,
            result.errors,
          );
        }
        expect(keywords).toContain(c.expectedError.keyword);
      }
    });
  }
});
