# Web client smoke example

A static page that loads `@poppy/client-web` from the built bundle and renders a small Poppy document. Clicking any button sends the action to the on-page log so you can see the host integration end-to-end.

## Run it

From the repo root:

```sh
pnpm -r build                # produces packages/client-web/dist/
python3 -m http.server 8000  # any static server works; python3 is universally available
```

Then open <http://localhost:8000/examples/web/>.

> **Why a server?** ESM imports don't work over the `file://` scheme in browsers (CORS). Any static HTTP server will do — `npx serve`, `caddy file-server`, `php -S`, etc.

## Edit it

Open `main.js` and modify the `document_` constant. The schema is at `packages/schema/schemas/poppy.schema.json` and the TypeScript types are at `packages/schema/src/types.ts`.

If you break the document, the renderer's default validation kicks in: nothing renders and the error appears in the browser console.
