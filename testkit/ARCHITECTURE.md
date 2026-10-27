# Testkit Module

Test-only helper module. Centralizes the "parse this string into tokens/statements" wiring
(`new StatementSyntaxReader(new Lexer(source))`) that test
fixtures across the repo need, without letting any *production* module depend on `lexer` just to
build test data.

Responsibilities:

- `TestSources.statementsOf(String)` — a `StatementSource` over a string.
- `TestSources.statementListOf(String)` — every statement of a string, as a list (drains
  `statementsOf`).
- `TestSources.tokensOf(String)` — a `TokenSource` over a string.

Design rules:

- Depends on [lexer](../lexer/ARCHITECTURE.md) and [ast](../ast/ARCHITECTURE.md). This is
  the one place in the codebase that is allowed to know both exist and wire them together — every
  other module should reach them only through the `TokenSource`/`StatementSource` ports.
- Only ever added as a `testImplementation` dependency, never `implementation`/`api`. If a main
  source set needs this module, that's a sign the pipeline wiring leaked out of the composition
  root ([toolchain](../toolchain/ARCHITECTURE.md)) and into a stage that shouldn't know about
  it.
- Keep it a thin convenience layer. It should not grow test assertions, fixtures with business
  meaning, or anything beyond "turn a string into pipeline data."

Representative test: `src/test/java/org/printscript/testkit/TestSourcesTest.java`.
