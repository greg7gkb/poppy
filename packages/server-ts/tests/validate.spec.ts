import { readFileSync, readdirSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { isValid, validate } from "../src/index.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const examplesDir = resolve(__dirname, "..", "..", "schema", "examples");

describe("validate (positive cases)", () => {
  for (const file of readdirSync(examplesDir).filter((n) => n.endsWith(".json"))) {
    it(`accepts example ${file}`, () => {
      const doc = JSON.parse(readFileSync(join(examplesDir, file), "utf-8"));
      const result = validate(doc);
      if (!result.ok) {
        console.error(file, result.errors);
      }
      expect(result.ok).toBe(true);
    });
  }
});

describe("validate (negative cases)", () => {
  it("rejects empty object", () => {
    expect(validate({}).ok).toBe(false);
  });

  it("rejects document missing root", () => {
    expect(validate({ version: "0.1" }).ok).toBe(false);
  });

  it("rejects document missing version", () => {
    expect(validate({ root: { type: "Text", value: "x" } }).ok).toBe(false);
  });

  it("rejects unknown component type", () => {
    const doc = {
      version: "0.1",
      root: { type: "Heading", value: "Hello" },
    };
    expect(validate(doc).ok).toBe(false);
  });

  it("rejects Image without alt", () => {
    const doc = {
      version: "0.1",
      root: { type: "Image", url: "https://example.com/x.png" },
    };
    expect(validate(doc).ok).toBe(false);
  });

  it("rejects Text with non-string value", () => {
    const doc = {
      version: "0.1",
      root: { type: "Text", value: 42 },
    };
    expect(validate(doc).ok).toBe(false);
  });

  it("rejects Button without action", () => {
    const doc = {
      version: "0.1",
      root: { type: "Button", label: "x" },
    };
    expect(validate(doc).ok).toBe(false);
  });

  it("rejects Stack with unknown axis", () => {
    const doc = {
      version: "0.1",
      root: { type: "Stack", axis: "diagonal", children: [] },
    };
    expect(validate(doc).ok).toBe(false);
  });
});

describe("isValid type guard", () => {
  it("returns true for a valid document", () => {
    const doc: unknown = { version: "0.1", root: { type: "Text", value: "Hi" } };
    expect(isValid(doc)).toBe(true);
  });

  it("returns false for an invalid document", () => {
    const doc: unknown = { version: "0.1" };
    expect(isValid(doc)).toBe(false);
  });
});
