import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

export interface ExpectedError {
  /** Schema keyword the error must be reported under (e.g. "required", "type", "discriminator", "enum"). */
  keyword: string;
  /** Optional JSON pointer to where the error is expected, e.g. "/root". */
  path?: string;
}

interface BaseCase {
  /** Directory name, e.g. "001-text-hello". Used as a stable test identifier. */
  name: string;
  /** Absolute path to the case directory. */
  dir: string;
  /** The Poppy document under test (parsed JSON). */
  document: unknown;
  /** Free-form description from description.md (trimmed). */
  description: string;
}

export interface ValidCase extends BaseCase {
  kind: "valid";
  /** Expected normalized HTML output for the Web renderer. Absent until Week 3 generates it. */
  webSnapshot?: string;
}

export interface InvalidCase extends BaseCase {
  kind: "invalid";
  /** What the validator must report for this case to pass. */
  expectedError: ExpectedError;
}

export type Case = ValidCase | InvalidCase;

export interface Corpus {
  valid: ValidCase[];
  invalid: InvalidCase[];
}

/**
 * Load the corpus from a directory containing `cases/valid/` and `cases/invalid/`.
 * Cases are returned sorted by directory name (which is conventionally `NNN-slug`).
 */
export function loadCases(corpusRoot: string): Corpus {
  const valid: ValidCase[] = [];
  const invalid: InvalidCase[] = [];

  for (const name of readdirSync(join(corpusRoot, "cases", "valid")).sort()) {
    const dir = join(corpusRoot, "cases", "valid", name);
    valid.push({
      kind: "valid",
      name,
      dir,
      document: readJson(join(dir, "document.json")),
      description: readText(join(dir, "description.md")),
      webSnapshot: readOptional(join(dir, "snapshot.web.html")),
    });
  }

  for (const name of readdirSync(join(corpusRoot, "cases", "invalid")).sort()) {
    const dir = join(corpusRoot, "cases", "invalid", name);
    invalid.push({
      kind: "invalid",
      name,
      dir,
      document: readJson(join(dir, "document.json")),
      description: readText(join(dir, "description.md")),
      expectedError: readJson(join(dir, "expected-error.json")) as ExpectedError,
    });
  }

  return { valid, invalid };
}

function readJson(path: string): unknown {
  return JSON.parse(readFileSync(path, "utf-8"));
}

function readText(path: string): string {
  return readFileSync(path, "utf-8").trim();
}

function readOptional(path: string): string | undefined {
  try {
    return readFileSync(path, "utf-8");
  } catch {
    return undefined;
  }
}
