# Diagnostics Module

Owns the shared vocabulary for reporting problems in user code: `Diagnostic`, `Severity`, `Phase`,
`DiagnosticCode` (a machine-readable reason, e.g. `UNEXPECTED_END_OF_INPUT`, for callers that must
react to one specific problem without matching on its message),
and `DiagnosticException`.

Design rules:

- Depends only on [source](../source/ARCHITECTURE.md) (`requires transitive`, since `Diagnostic`
  exposes `SourceSpan` in its public API).
- User-code problems are diagnostics, not exceptions. Every phase of the pipeline (lexer, parser,
  typechecker, interpreter, analyzer) should report failures as a `Diagnostic` with a severity,
  message, `SourceSpan`, and `Phase`. `DiagnosticException` is the one exception every stage throws,
  and only a transport mechanism for propagating diagnostics up to the caller, never a substitute
  for them; the phase on each diagnostic says which stage failed.
- Do not add anything here that isn't part of that vocabulary. Orchestration-only concepts
  (results, versions, progress reporting) do not belong in this module — see
  [Toolchain Module](../toolchain/ARCHITECTURE.md).

Representative tests: `src/test/java/org/printscript/diagnostics/DiagnosticTest.java`,
`DiagnosticExceptionTest.java`.
