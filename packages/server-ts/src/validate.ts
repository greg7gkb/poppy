import type { PoppyDocument } from "@poppy/schema";
import validateFn from "./generated/validator.js";

export interface ValidationError {
  /** JSON pointer to the location of the failure, e.g. "/root/children/0/value". */
  path: string;
  /** Human-readable message from the underlying validator. */
  message: string;
  /** The schema keyword that failed (type, required, enum, oneOf, etc.). */
  keyword: string;
}

export type ValidationResult =
  | { ok: true; document: PoppyDocument }
  | { ok: false; errors: ValidationError[] };

/**
 * Validate a Poppy document against the v0.1 schema.
 * Returns a discriminated result. Never throws on invalid input.
 */
export function validate(doc: unknown): ValidationResult {
  const ok = validateFn(doc);
  if (ok) {
    return { ok: true, document: doc as PoppyDocument };
  }
  const errors: ValidationError[] = (validateFn.errors ?? []).map((e) => ({
    path: e.instancePath || "/",
    message: e.message ?? "validation failed",
    keyword: e.keyword,
  }));
  return { ok: false, errors };
}

/** Type guard form of {@link validate}. */
export function isValid(doc: unknown): doc is PoppyDocument {
  return validateFn(doc) === true;
}
