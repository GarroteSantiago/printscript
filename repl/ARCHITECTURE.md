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

- `Repl`: the composition root and entry point (`main`). Reads one line at a time from a
  `Reader`/`PrintStream` trio — `System.in`/`System.out`/`System.err` in production, in-memory
  streams in tests — buffering lines until `isOpenBlock` says the buffer looks complete, then
  parses and runs it via `ReplSession` (see below).
- `ReplSession`: owns the `SemanticContext`/`RuntimeEnvironment` state across statements. `step`
  takes one already-parsed statement, validates and executes it, and never throws — a semantic
  error or a runtime failure is reported as diagnostics, with the session's state left exactly as
  it was before the failing statement.
- `SingleValidatedStatementSource`: adapts one already-validated statement into the
  `typetable.ValidatedStatementSource` port `Interpreter#executeAll` expects, so `ReplSession` can
  execute one statement at a time through that exact same public entry point instead of needing a
  new interpreter API.

**Why line-buffered, not streamed.** The obvious design — feed the `Reader` straight into one
persistent `Lexer`/`StatementSyntaxReader` pair for the whole session — seems to handle a multi-line
`if (...) { ... }` block "for free": the parser's lookahead just blocks on the `Reader` for more
input. It was tried first, and doesn't work well: `StatementSyntaxReader` always reads one token
*past* whatever it just matched (its constructor fetches two tokens up front, and every `advance()`
refills one more), so confirming statement N complete always requires blocking to read statement
N+1's first token. Verified by hand, not a hypothetical: every statement's prompt and its own output
landed one command late — typing `let a: string = "A";` produced no visible response at all until
the *next* line was typed, at which point the prompt and output for the first statement finally
appeared. `Repl` instead buffers whole lines (`BufferedReader#readLine`) and only invokes the
`Lexer`/`StatementSyntaxReader` once `isOpenBlock` (a small pre-scan counting `{`/`}` tokens) says
the buffer isn't sitting inside an unclosed block. Every real parse then runs over an in-memory,
already-fully-read `StringReader` with a definite end — never blocking mid-parse — so prompts and
output appear exactly when they should, and a syntax error is a normal, bounded parse failure rather
than something that can leave shared parser state to recover from. A malformed line's buffer is
simply discarded as one atomic unit and the next line starts completely fresh, which also means
syntax-error recovery is now exact: no tokens from the following statement are ever at risk of being
silently lost the way an incrementally-resynced streaming parser could lose them.

Representative tests: `src/test/java/org/printscript/repl/ReplSessionTest.java` (state threading,
error recovery), `src/test/java/org/printscript/repl/ReplTest.java` (the full loop over in-memory
streams, including a block statement spanning several lines, and — via a real `PipedWriter`/background
thread, since a fully-materialized `StringReader` can't distinguish "late" from "eventual" — a
regression test asserting the next prompt is already printed before the next line is typed).
