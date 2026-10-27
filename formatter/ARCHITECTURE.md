# Formatter Module

The formatter rewrites source using immutable formatter configuration. It is a targeted trivia
rewrite over the token stream, not a pretty-printer: every token's text is re-emitted byte for
byte, and only the whitespace a configured rule controls is rewritten.

The formatter should preserve comments and untouched whitespace. It should only rewrite trivia controlled by configured formatter rules.

For example, if a rule controls spacing after `(` but no rule controls spacing before `)`, the formatter should update the first spacing point and leave the second one unchanged.

Formatter input:

```text
SourceInput
  -> Lexer
  -> TokenStream with Trivia
  -> Rewrite Selected Trivia
  -> Formatted Source
```

Design rules:

- Depends only on [tokens](../tokens/ARCHITECTURE.md). Its input is a `TokenSource`, its output is
  text; it never sees a parser or a tree. The little structure its rules need (block nesting depth
  for indentation, and whether the previous top-level statement was a `println` call) is tracked
  from the tokens themselves.
- Formatting never validates. Whether a token sequence is a valid program is the `validate`
  command's job, so syntactically invalid code still formats (best effort; stray braces never drive
  the indentation depth below zero). Only a lexical error, raised by the `TokenSource`, stops it.

Configuration should cover:

- spaces before semicolons
- spaces after semicolons
- spaces around assignment
- spaces around operators
- blank lines after `println` (the `line-breaks-after-println` key)
- line breaks after statements

Comments and whitespace are syntax trivia, not syntactic sugar.

## Swappable per-version behavior

`SpacingRules` decides how much leading trivia precedes each token, given its `TokenType`, the
previous token's `TokenType`, and `FormatterConfig`. It is constructor-injected into
`PrintScriptFormatter` (default `v1()`), selected by the composition root in
[toolchain](../toolchain/ARCHITECTURE.md), instead of being a hardcoded `switch` over token
types. A version that adds a new token kind needing its own spacing rule extends `SpacingRules`, not
`PrintScriptFormatter`'s dispatch logic.

Representative tests: `src/test/java/org/printscript/formatter/PrintScriptFormatterTest.java`
(v1 spacing/trivia rules), `PrintScriptFormatterV11Test.java` (block indentation and brace
placement added in v1.1).
