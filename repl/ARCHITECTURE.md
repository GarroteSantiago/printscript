# REPL Module

The `repl` module is a second interaction adapter, alongside
[CLI Module](../cli/ARCHITECTURE.md) — an interactive read-eval-print loop instead of a one-shot
file command. See the [root README](../README.md) and
[docs/architecture.md](../docs/architecture.md) for how this fits into the rest of the system.

Unlike `cli`, `repl` does not depend on `toolchain`. `toolchain.PrintScript`'s public methods
(`execute`/`format`/`analyze`/`validate`) each consume a whole program in one call and construct a
fresh `SemanticContext`/`RuntimeEnvironment` internally — there is no way to get state back out
between statements, because batch execution never needs to. A REPL needs exactly that: to thread a
`SemanticContext` and a `RuntimeEnvironment` across many separate statements typed one at a time.
Rather than growing `toolchain`'s API with a REPL-specific stateful session type, `repl` is its own
thin composition root, depending on the language core directly (`lexer`, `parser`, `typechecker`,
`interpreter`) the same way `toolchain` does — reusing the exact same public, already-stateless
primitives (`SemanticContext#validate`, `Interpreter#executeAll`) `PrintScript.execute` builds on
internally. `toolchain` and the language core stay exactly as stateless as they already were; only
`repl` itself holds any session state, in `ReplSession`.

Responsibilities:

- `Repl`: the composition root and entry point (`main`). Wires a `Reader`/`PrintStream` trio —
  `System.in`/`System.out`/`System.err` in production, in-memory streams in tests — into a
  persistent `Lexer`/`StatementSyntaxReader` pair and a `ReplSession`, and drives the prompt loop.
- `ReplSession`: owns the `SemanticContext`/`RuntimeEnvironment` state across statements. `step`
  parses, validates, and executes exactly one statement and never throws — a syntax error, a
  semantic error, or a runtime failure are all reported as diagnostics, with the session's state
  left exactly as it was before the failing statement.
- `SingleValidatedStatementSource`: adapts one already-validated statement into the
  `typetable.ValidatedStatementSource` port `Interpreter#executeAll` expects, so `ReplSession` can
  execute one statement at a time through that exact same public entry point instead of needing a
  new interpreter API.

Because `Repl.run` reads from an ordinary `Reader`, a multi-line block (`if (...) { ... }` spanning
several typed lines) is handled for free: the parser's lookahead simply blocks for more input from
that `Reader` exactly as it already does for `toolchain.PrintScript` reading a whole file — no
REPL-specific buffering logic was needed for this.

Known rough edge: after a syntax error, the underlying `StatementSyntaxReader`'s one-token lookahead
may not be sitting cleanly at the next statement's first token, so a badly malformed line can
cascade into a second, spurious diagnostic before the session recovers. The language core
deliberately has no parser recovery for batch compilation; a REPL that resyncs perfectly after any
malformed input is future work.

Representative tests: `src/test/java/org/printscript/repl/ReplSessionTest.java` (state threading,
error recovery), `src/test/java/org/printscript/repl/ReplTest.java` (the full loop over in-memory
streams, including a block statement spanning several `Reader` lines).
