# Formatter Module

The formatter rewrites source using immutable formatter configuration.

For version `1.0.0`, formatting should be a targeted lossless CST/trivia rewrite.

The formatter should preserve comments and untouched whitespace. It should only rewrite trivia controlled by configured formatter rules.

For example, if a rule controls spacing after `(` but no rule controls spacing before `)`, the formatter should update the first spacing point and leave the second one unchanged.

Formatter input:

```text
SourceInput
  -> Lexer
  -> TokenStream with Trivia
  -> Lossless CST
  -> Rewrite Selected Trivia
  -> Formatted Source
```

Configuration should cover:

- spaces before semicolons
- spaces after semicolons
- spaces around assignment
- spaces around operators
- blank lines before `println`
- line breaks after statements

Comments and whitespace are syntax trivia, not syntactic sugar.

The formatter may consult abstract syntax when a rule needs language meaning, but its default job is to rewrite selected trivia and preserve everything else.

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
