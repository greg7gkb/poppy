# Web client smoke example

A static page that loads `@poppy/client-web` from the built bundle and renders a small Poppy document. Clicking any button sends the action to the on-page log so you can see the host integration end-to-end.

## Run it

```sh
# Build the bundle (only needed once, or after changes to client-web)
pnpm -r build

# Start a local server. Use the bundled script — it works from anywhere.
cd examples/web
./run_server.sh
```

Then open <http://localhost:8000/examples/web/>.

> **Why a server?** ESM imports don't work over `file://` in browsers (CORS).
>
> **Why not raw `python3 -m http.server`?** This page imports the client bundle via `../../packages/client-web/dist/...`. Python's `http.server` serves files relative to the directory it's launched from — running it from `examples/web/` makes those `../../` paths escape the document root and 404. `run_server.sh` solves this by always serving from the repo root. Equivalent one-liner: `python3 -m http.server 8000 --directory ../..` (run from this directory).

Any static server works: `npx serve`, `caddy file-server`, `php -S` — as long as it's rooted at the repo root, not at `examples/web/`.

## Edit it

Open `main.js` and modify the `document_` constant. The schema is at `packages/schema/schemas/poppy.schema.json` and the TypeScript types are at `packages/schema/src/types.ts`.

If you break the document, the renderer's default validation kicks in: nothing renders and the error appears in the browser console.
