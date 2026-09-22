# Source Module

The lowest-level module in the system: pure text-position primitives.

Responsibilities:

- `SourcePosition` (row, column, offset)
- `SourceSpan` (a start/end pair of positions)

Design rules:

- Zero dependencies. Every other module in the system may end up needing "where in the source
  did this happen," so this module must never depend on anything above it — not even
  `diagnostics`.
- Immutable value types only. No behavior beyond simple invariants (e.g. positions cannot be
  negative).

Representative tests: `src/test/java/org/printscript/source/SourcePositionTest.java`,
`SourceSpanTest.java`.
