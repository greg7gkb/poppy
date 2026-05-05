# ADR-0003: Apache License 2.0

## Status

Accepted — 2026-05-05

## Context

We need to choose an open-source license that:

1. Maximizes adoption — especially by companies and other OSS projects.
2. Provides reasonable patent protection. UI rendering algorithms, layout models, and serialization patterns can be subject to patents; an SDUI library is exactly the kind of code where this matters.
3. Is simple and well-understood.
4. Has no copyleft requirements that would deter corporate adoption of the library inside proprietary products.

## Decision

Poppy is licensed under the **Apache License, Version 2.0**.

- Full license text in [`LICENSE`](../../LICENSE) at repo root.
- Attribution stub in [`NOTICE`](../../NOTICE) at repo root.
- New source files include a short SPDX-style header (`SPDX-License-Identifier: Apache-2.0`) once we begin committing code in Phase 1.

## Consequences

**Positive**

- Includes an explicit grant of contributor patents to users — the core advantage over MIT/BSD for a UI library.
- Includes a patent retaliation clause: if an entity initiates patent litigation over Poppy, their patent license terminates. This is a meaningful disincentive against patent trolling.
- Compatible with most other licenses; corporate legal teams are familiar with it.
- Structured rules around `NOTICE` files improve attribution traceability for downstream redistributors.
- No copyleft burden — adopters can embed Poppy in proprietary products freely.

**Negative**

- Slightly more verbose than MIT for redistribution requirements (`NOTICE` preservation, marking modifications).
- Apache 2.0 is incompatible with combining Poppy source into a GPLv2-only project (it is compatible with GPLv3).

## Alternatives Considered

| License | Permissive? | Patent grant? | Notes |
|---|---|---|---|
| MIT | Yes | No | Simpler text; widely adopted. Lacks the patent protection that matters for a UI lib. |
| BSD-3-Clause | Yes | No | Similar to MIT plus a non-endorsement clause. Same gap on patents. |
| **Apache 2.0** | **Yes** | **Yes** | **Chosen.** Permissive plus patent grant and retaliation. |
| MPL 2.0 | File-level copyleft | Yes | Reasonable middle ground; the file-level copyleft creates friction for derivative works. |
| GPLv3 / AGPL | Strong copyleft | Yes | Would deter most corporate adoption. |
| Unlicense / 0BSD | Maximally permissive | No | No patent protection; enforceability is uneven across jurisdictions. |

## References

- Apache License 2.0 text: https://www.apache.org/licenses/LICENSE-2.0
- SPDX identifier: `Apache-2.0`
- Comparison of permissive licenses: https://choosealicense.com/
