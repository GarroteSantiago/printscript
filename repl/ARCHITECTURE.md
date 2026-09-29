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
  persistent `Lexer` and a `ReplSession`, and drives the prompt loop. Also owns syntax-error
  recovery (see below); parsing is not `ReplSession`'s job precisely because that recovery decision
  belongs to whoever constructs the `Lexer`/`StatementSyntaxReader` pair.
- `ReplSession`: owns the `SemanticContext`/`RuntimeEnvironment` state across statements. `step`
  takes one already-parsed statement, validates and executes it, and never throws — a semantic
  error or a runtime failure is reported as diagnostics, with the session's state left exactly as
  it was before the failing statement.
- `SingleValidatedStatementSource`: adapts one already-validated statement into the
  `typetable.ValidatedStatementSource` port `Interpreter#executeAll` expects, so `ReplSession` can
  execute one statement at a time through that exact same public entry point instead of needing a
  new interpreter API.

Because `Repl.run` reads from an ordinary `Reader`, a multi-line block (`if (...) { ... }` spanning
several typed lines) is handled for free: the parser's lookahead simply blocks for more input from
that `Reader` exactly as it already does for `toolchain.PrintScript` reading a whole file — no
REPL-specific buffering logic was needed for this.

**Syntax-error recovery and its limits.** A naive REPL that just kept calling `next()` on the same
`StatementSyntaxReader` after a syntax error would hang forever: that reader's one-token lookahead
can get stuck sitting on a token that can't start any statement, so every subsequent call throws the
identical exception without ever advancing — verified directly, this is not a hypothetical. `Repl`
recovers by rebuilding the `StatementSyntaxReader` wrapper (not the `Lexer` underneath it — the
`Lexer` is always left correctly positioned right after whatever character failed it, so discarding
it too would lose more input than necessary) on every syntax error, which guarantees the read loop
always makes forward progress and eventually reaches real EOF. This is not a lossless recovery,
though: a simple, self-contained bad token (a stray character forming its own malformed
"statement") recovers cleanly, with the next statement running normally. A syntax error embedded
inside a multi-token compound statement (e.g. an unsupported operator inside an `if` condition) can
cost several subsequent tokens — occasionally an entire following statement — before the loop
resynchronizes, because each rebuild discards whatever lookahead the previous attempt had already
fetched, and there's no way to ask the parser to back up rather than discard. Actually fixing that
would mean giving `parser.StatementSyntaxReader` some form of external resynchronization, which
conflicts with the language core's stated "no parser recovery" design and would need to be decided
at that level, not patched around here.

Representative tests: `src/test/java/org/printscript/repl/ReplSessionTest.java` (state threading,
error recovery), `src/test/java/org/printscript/repl/ReplTest.java` (the full loop over in-memory
streams, including a block statement spanning several `Reader` lines).
