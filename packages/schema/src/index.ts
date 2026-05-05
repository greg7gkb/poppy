// Public entry for `@poppy/schema`.
//
// Phase 1: exports TypeScript types. The JSON Schema files live at
// `../schemas/` and are loaded directly by the build-time validator
// pipeline in `@poppy/server-ts`.
//
// A future Phase 1 tooling session will:
//   - add a `package.json` with proper exports (including `./schemas/*.json`)
//   - wire the package into the pnpm workspace
//   - publish to npm under `@poppy/schema`

export * from "./types";
