import { type PoppyDocument, SCHEMA_VERSION } from "@poppy/schema";
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

const [SUPPORTED_MAJOR, SUPPORTED_MINOR] = parseVersion(SCHEMA_VERSION);

/**
 * Validate a Poppy document against the v0.1 schema.
 * Returns a discriminated result. Never throws on invalid input.
 *
 * Order of checks:
 *   1. JSON Schema (structure, types, enums, required fields).
 *   2. Wire-format version compatibility (ADR-0006: matching major, minor <= renderer).
 *
 * Version compat is checked AFTER schema validation, but only if schema passed —
 * a malformed `version` field is reported as a schema error, not a compat error.
 */
export function validate(doc: unknown): ValidationResult {
  const ok = validateFn(doc);
  if (!ok) {
    const errors: ValidationError[] = (validateFn.errors ?? []).map((e) => ({
      path: e.instancePath || "/",
      message: e.message ?? "validation failed",
      keyword: e.keyword,
    }));
    return { ok: false, errors };
  }

  // Schema passed, so we know `version` is a string matching `^\d+\.\d+$`.
  const versionError = checkVersionCompat((doc as PoppyDocument).version);
  if (versionError) {
    return { ok: false, errors: [versionError] };
  }

  return { ok: true, document: doc as PoppyDocument };
}

/** Type guard form of {@link validate}. */
export function isValid(doc: unknown): doc is PoppyDocument {
  return validate(doc).ok;
}

function parseVersion(v: string): [number, number] {
  const [maj, min] = v.split(".");
  return [Number(maj), Number(min)];
}

function checkVersionCompat(version: string): ValidationError | null {
  const [major, minor] = parseVersion(version);
  if (major !== SUPPORTED_MAJOR) {
    return {
      path: "/version",
      keyword: "version",
      message: `unsupported document major version "${version}"; this renderer supports ${SUPPORTED_MAJOR}.x`,
    };
  }
  if (minor > SUPPORTED_MINOR) {
    return {
      path: "/version",
      keyword: "version",
      message: `document minor version ${minor} exceeds renderer's supported minor ${SUPPORTED_MINOR}`,
    };
  }
  return null;
}
