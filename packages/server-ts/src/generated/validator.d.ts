// Type contract for the generated Ajv standalone validator.
//
// The implementation lives in validator.js, generated at build time by
// scripts/compile-schema.mjs and gitignored. This .d.ts is committed so the
// package type-checks before any build has run.

interface AjvError {
  instancePath: string;
  schemaPath: string;
  keyword: string;
  params: Record<string, unknown>;
  message?: string;
}

interface AjvValidator {
  (data: unknown): boolean;
  errors?: AjvError[] | null;
}

declare const validate: AjvValidator;
export default validate;
