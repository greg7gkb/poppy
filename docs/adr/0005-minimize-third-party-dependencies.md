# ADR-0005: Minimize third-party runtime dependencies

## Status

Accepted — 2026-05-05

## Context

Poppy is an open-source library that adopters embed in their products across four languages. Every transitive dependency Poppy adds:

- Becomes a permanent maintenance commitment (security advisories, version bumps, breaking changes).
- Increases the supply-chain surface for adopters.
- May conflict with the adopter's own dependency graph.
- Bloats the runtime footprint, which matters especially on mobile.

OSS libraries in this space are routinely criticized — sometimes fatally — for over-aggressive dependency pulling. We want to start with a stricter posture than we expect to need and relax it deliberately if pressure justifies.

## Decision

Poppy applies a **high bar for third-party runtime dependencies.** A pull request that adds a new runtime dependency in any package must:

1. Open a discussion (issue or draft PR) before merging.
2. Justify the dependency against the criteria below.
3. If accepted, document the choice in a brief ADR in `docs/adr/`.

### Criteria for acceptance

A new runtime dependency is accepted if **most** of these hold:

- Implementing the functionality ourselves would take significant effort or ongoing maintenance (e.g. JSON Schema validation, cryptography).
- The dependency is widely adopted, actively maintained, and has a stable release history.
- The dependency has a permissive license compatible with Apache 2.0.
- The dependency does not pull in a large transitive dependency tree.
- The surface area we use is small and well-defined.

Dev-only dependencies (linters, formatters, test runners, build tools) are less constrained but still warrant a brief justification in the PR description.

## Consequences

**Positive**

- Smaller install footprint and faster builds for adopters.
- Lower supply-chain risk and lower long-term maintenance burden.
- Forces conscious thought about each dependency, which usually surfaces lighter alternatives.

**Negative**

- More implementation work upfront for utilities we might otherwise pull from a library.
- Some categories — notably JSON Schema validation across four languages — genuinely require dependencies; we accept those with proper justification.
- Strict gates can frustrate contributors. The CONTRIBUTING document explains the rule and links here.

## Alternatives Considered

- **No restrictions.** Rejected: the typical pattern in OSS, and the typical pain point. We are explicitly choosing the harder, smaller-surface path.
- **Total prohibition.** Rejected: pragmatically infeasible for cross-language JSON Schema validation in Phase 1.

## References

- [`CONTRIBUTING.md`](../../CONTRIBUTING.md) — encodes this policy as one of the two bright-line rules.
