// API-level smoke tests for validate() and isValid().
// Schema-level coverage (per-component, per-token) lives in corpus.spec.ts,
// which exercises the @poppy/conformance corpus.

import { describe, expect, it } from "vitest";
import { isValid, validate } from "../src/index.js";

describe("validate() API", () => {
  it("returns ok:true with the typed document for a valid input", () => {
    const doc = { version: "0.1", root: { type: "Text", value: "Hi" } };
    const result = validate(doc);
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.document).toEqual(doc);
    }
  });

  it("returns ok:false with errors for invalid input", () => {
    const result = validate({});
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.errors[0]).toHaveProperty("keyword");
      expect(result.errors[0]).toHaveProperty("message");
      expect(result.errors[0]).toHaveProperty("path");
    }
  });

  it("never throws on garbage input", () => {
    for (const garbage of [null, undefined, 0, "", [], "not-json-but-a-string"]) {
      expect(() => validate(garbage)).not.toThrow();
    }
  });

  it("rejects a document missing version", () => {
    expect(validate({ root: { type: "Text", value: "x" } }).ok).toBe(false);
  });

  it("rejects a document missing root", () => {
    expect(validate({ version: "0.1" }).ok).toBe(false);
  });

  it("rejects a Stack with an unknown axis token", () => {
    const doc = {
      version: "0.1",
      root: { type: "Stack", axis: "diagonal", children: [] },
    };
    expect(validate(doc).ok).toBe(false);
  });
});

describe("version compatibility (ADR-0006)", () => {
  it("accepts the renderer's exact version", () => {
    const doc = { version: "0.1", root: { type: "Text", value: "x" } };
    expect(validate(doc).ok).toBe(true);
  });

  it("rejects an unknown major with keyword 'version' at /version", () => {
    const doc = { version: "999.0", root: { type: "Text", value: "x" } };
    const result = validate(doc);
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.errors).toHaveLength(1);
      expect(result.errors[0].keyword).toBe("version");
      expect(result.errors[0].path).toBe("/version");
    }
  });

  it("rejects a future minor within the supported major", () => {
    const doc = { version: "0.99", root: { type: "Text", value: "x" } };
    const result = validate(doc);
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.errors[0].keyword).toBe("version");
      expect(result.errors[0].path).toBe("/version");
    }
  });

  it("reports schema errors (not version errors) when version is malformed", () => {
    const doc = { version: "abc", root: { type: "Text", value: "x" } };
    const result = validate(doc);
    expect(result.ok).toBe(false);
    if (!result.ok) {
      const keywords = new Set(result.errors.map((e) => e.keyword));
      expect(keywords).not.toContain("version");
    }
  });
});

describe("isValid() API", () => {
  it("returns true for a valid document", () => {
    const doc: unknown = { version: "0.1", root: { type: "Text", value: "Hi" } };
    expect(isValid(doc)).toBe(true);
  });

  it("returns false for an invalid document", () => {
    expect(isValid({ version: "0.1" })).toBe(false);
  });

  it("never throws on garbage", () => {
    for (const garbage of [null, undefined, 0, "", []]) {
      expect(() => isValid(garbage)).not.toThrow();
    }
  });
});
