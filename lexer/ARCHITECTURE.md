# Lexer Module

The lexer module turns raw source text into tokens. It is the scanning implementation — the token
contract it produces lives one level below it, in [tokens](../tokens/ARCHITECTURE.md).

Responsibilities:

- `Lexer` — character-level scanning, implementing `tokens.TokenSource`.

Design rules:

- Depend only on `tokens` (`requires transitive`, since `Lexer implements TokenSource` is part of
  this module's public API). Never depend on `syntax` — `syntax` depends on `tokens`, the same as
  this module does, and the two must not depend on each other.
- Consumers (the composition root in [application](../application/ARCHITECTURE.md), or
  [testkit](../testkit/ARCHITECTURE.md) for tests) construct a concrete `Lexer` and hand it to
  anything that wants a `TokenSource`. Nothing should reach for the `Lexer` class by name except at
  that wiring boundary — everywhere else should type against `TokenSource`.
- No parser recovery: fail fast with a `SyntaxException` on the first invalid character or
  unterminated literal rather than trying to resynchronize.
- Comments and whitespace are trivia, not syntactic sugar; keep them attached to tokens so the
  formatter and any future comment-preserving tooling can recover them losslessly.

## Swappable per-version behavior

The keyword table (which lexemes are `let`/`number`/`string` vs. plain identifiers) is not
hardcoded in `Lexer`'s scanning logic. It lives behind `KeywordTable`, constructor-injected into
`Lexer` (default `v1()`), selected by the composition root in
[application](../application/ARCHITECTURE.md). A future version adding keywords only needs a new
`KeywordTable`, not a change to `Lexer` itself.

Representative tests: `src/test/java/org/printscript/lexer/LexerTest.java` (scanning behavior),
`KeywordTableTest.java`/`KeywordTableBuilderTest.java` (per-version keyword sets).
