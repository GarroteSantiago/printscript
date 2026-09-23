# Parser Module

The parser module is the sole production implementation of `ast.StatementSource`. It owns
turning a token stream into the AST that [ast](../ast/ARCHITECTURE.md) defines — nothing
else in this repo builds a tree.

Responsibilities:

- `StatementSyntaxReader` — a recursive-descent parser with one-token lookahead, pulling from a
  `tokens.TokenSource` (never a concrete `Lexer` itself, so it works against any token producer)
- `SyntaxTreeBuilder` — drains a `StatementSource` into a fully materialized `ProgramSyntax`, for
  callers that want the whole tree at once instead of streaming statement-by-statement

Split out from `ast` on purpose. `formatter`, `interpreter`, and `analyzer` all walk the AST
`ast` defines, but none of them parse — they receive an already-built tree from `application`'s
composition root, one statement at a time. Before this split, those three modules pulled in the
parser transitively through `ast` (then still named `syntax`) even though they never referenced
it. Depending on `parser` at
all is now a signal that a consumer builds trees, not just walks them; today that's only
`application` (the composition root) and [testkit](../testkit/ARCHITECTURE.md) (test fixtures).

```text
TokenSource (Lexer)
  -> StatementSyntaxReader (implements StatementSource)
  -> Statement, pulled one at a time
```

`SyntaxTreeBuilder` is not the production path — `application.PrintScript` validates and
processes each statement as it's parsed, interleaved with execution/formatting/analysis, so a
later statement is never even parsed once an earlier one has failed. `SyntaxTreeBuilder` exists for
callers that genuinely want the whole program up front, which today is only
`testkit.TestSources#programOf` and this module's own tests.

There is no error recovery: the first malformed construct throws a `tokens.SyntaxException` and
parsing stops. If recovery is needed later, semicolon should be the main synchronization point.

Representative tests: `ParserTest.java` (v1 grammar), `ParserV11Test.java`
(`const`/`if`/`else`/boolean literals added in v1.1), `ParserDeclarationTest.java` (variable
declaration edge cases), `StatementSyntaxReaderTest.java`, `SyntaxTreeBuilderTest.java`.
