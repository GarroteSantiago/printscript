# Tokens Module

Owns the raw token-streaming contract shared by every stage that produces or consumes tokens, plus
the closed set of token kinds those tokens carry.

Responsibilities:

- `Token` — raw lexer output.
- `TokenType`, `TokenTypeVisitor` — the closed set of token kinds, dispatched via visitor rather
  than a Java `enum`/`switch` (see the "Recurring pattern" section in
  [docs/architecture.md](../docs/architecture.md)).
- `TokenSource` — the pull-based port (`Token next()`) a token producer implements and a token
  consumer depends on. This is what lets [lexer](../lexer/ARCHITECTURE.md) and
  [parser](../parser/ARCHITECTURE.md) avoid depending on each other directly: `lexer` depends on
  `tokens` to implement `TokenSource`, `parser` depends on `tokens` to consume it, and neither
  depends on the other.
- `SyntaxToken` — the immutable, trivia-carrying token shape the AST retains, built from a `Token`
  via `SyntaxToken.from(Token)`.

Design rules:

- Depends only on [source](../source/ARCHITECTURE.md) (`requires transitive`, since `Token`
  exposes `SourceSpan`). Syntax errors are `diagnostics.DiagnosticException`s thrown by the lexer
  and parser, not a type owned here, so this module doesn't depend on `diagnostics`.
- Never depend on `lexer` or `parser`. This module exists specifically so those two do not need to
  depend on each other.

## Why lexer and parser don't depend on each other

```mermaid
graph TD
    Lexer["lexer.Lexer\n(implements TokenSource)"] -->|depends on| Tokens["tokens\n(TokenSource, Token, TokenType)"]
    Parser["syntax.StatementSyntaxReader\n(consumes TokenSource)"] -->|depends on| Tokens
    Toolchain["toolchain.PrintScript\n(composition root)"] -.constructs.-> Lexer
    Toolchain -.injects Lexer as TokenSource into.-> Parser
```

`lexer` and `parser` each depend only on this module, never on each other. The composition root in
[toolchain](../toolchain/ARCHITECTURE.md) is the only place that knows both concrete types
exist and wires one into the other; every module in between types against `TokenSource`.

Representative tests: `src/test/java/org/printscript/tokens/TokenTypeTest.java`,
`SyntaxTokenTest.java`.
