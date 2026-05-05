# `@poppy/client-web`

Native HTML / DOM renderer for Poppy. **The reference renderer.**

## Status

Planned for **Phase 1**. Currently a placeholder.

## Why "reference renderer"

The web client is the first renderer to be implemented and the one that the other renderers must visually agree with. It is also the renderer wired into the live playground in our docs site.

The web renderer takes a Poppy document and produces HTML elements directly. It does not depend on React or any UI framework — just the platform DOM API. The optional React renderer ([`@poppy/client-react`](../client-react/)) is a separate package for adopters who want React-ergonomic integration.

## See also

- [ADR-0004 — Conformance corpus](../../docs/adr/0004-conformance-corpus.md)
