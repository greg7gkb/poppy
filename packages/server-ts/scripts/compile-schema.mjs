// Compile the Poppy JSON Schema files into a standalone Ajv validator.
//
//   - Loads every *.json under packages/schema/schemas/ and registers them with Ajv 2020.
//   - Smoke-validates every example under packages/schema/examples/ as a sanity check.
//   - Emits a precompiled standalone validator at src/generated/validator.js (gitignored).
//
// Run via `pnpm compile-schema` from packages/server-ts/. Build and test pipelines invoke
// this transitively before running tsup or vitest.

import { mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import Ajv2020Mod from "ajv/dist/2020.js";
import standaloneCodeMod from "ajv/dist/standalone/index.js";

// ESM/CJS interop defensiveness — Ajv is published as CJS and Node's
// interop shape varies across versions.
const Ajv2020 = Ajv2020Mod.default ?? Ajv2020Mod;
const standaloneCode = standaloneCodeMod.default ?? standaloneCodeMod;

const __dirname = dirname(fileURLToPath(import.meta.url));
const schemaDir = resolve(__dirname, "..", "..", "schema", "schemas");
const examplesDir = resolve(__dirname, "..", "..", "schema", "examples");
const outDir = resolve(__dirname, "..", "src", "generated");

const ROOT_ID =
  "https://raw.githubusercontent.com/greg7gkb/poppy/v0.1.0/packages/schema/schemas/poppy.schema.json";

function loadJsonFiles(dir) {
  const out = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, entry.name);
    if (entry.isDirectory()) {
      out.push(...loadJsonFiles(p));
    } else if (entry.name.endsWith(".json")) {
      out.push({ path: p, data: JSON.parse(readFileSync(p, "utf-8")) });
    }
  }
  return out;
}

const allSchemas = loadJsonFiles(schemaDir).map((f) => f.data);
const examples = loadJsonFiles(examplesDir);

console.log(`Loaded ${allSchemas.length} schema file(s) from ${schemaDir}`);
console.log(`Loaded ${examples.length} example document(s) from ${examplesDir}`);

const ajv = new Ajv2020({
  schemas: allSchemas,
  code: { source: true, esm: true },
  discriminator: true,
  strict: false,
  allErrors: true,
});

const validate = ajv.getSchema(ROOT_ID);
if (!validate) {
  console.error(`\nERROR: could not load root schema validator for ${ROOT_ID}`);
  console.error("Check that packages/schema/schemas/poppy.schema.json has the expected $id.");
  process.exit(1);
}

console.log("\nValidating examples against the schema:");
let allOk = true;
for (const ex of examples) {
  const ok = validate(ex.data);
  const name = ex.path.split("/").slice(-1)[0];
  if (ok) {
    console.log(`  OK   ${name}`);
  } else {
    allOk = false;
    console.error(`  FAIL ${name}`);
    for (const err of validate.errors ?? []) {
      console.error(`         ${err.instancePath || "(root)"}: ${err.message} [${err.keyword}]`);
    }
  }
}
if (!allOk) {
  console.error("\nSchema validation failed for one or more examples.");
  process.exit(1);
}

const moduleCode = standaloneCode(ajv, validate);
mkdirSync(outDir, { recursive: true });
const outPath = join(outDir, "validator.js");
writeFileSync(outPath, moduleCode);
console.log(`\nWrote ${outPath} (${moduleCode.length} bytes)`);
