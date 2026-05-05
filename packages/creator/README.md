# `@poppy/creator`

Web-based design tool for authoring Poppy documents.

## Status

Planned for **Phase 3**. Currently a placeholder.

## Why later

The creator depends on a stable schema. Building it before the schema settles means rewriting it repeatedly. We will not start this until Phases 1 and 2 land and the schema is stable enough to commit to.

## Design intent (sketch)

- Drag-and-drop or tree-based document editor.
- Live preview using `@poppy/client-web` (the reference renderer).
- Export the document JSON for plumbing into a server.
