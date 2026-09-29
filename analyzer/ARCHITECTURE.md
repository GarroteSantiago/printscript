# Analyzer Module

The analyzer runs configurable static analysis rules.

Responsibilities:

- style diagnostics
- policy diagnostics
- configurable analyzer rules
- exact source positions for findings

Semantic correctness belongs to [typechecker](../typechecker/ARCHITECTURE.md)/[typetable](../typetable/ARCHITECTURE.md). The analyzer should focus on style and policy.

Example rules:

- identifier naming style
- invalid `println` argument shape
- enabled or disabled rules based on config

Analyzer rules should receive AST messages through focused receivers where useful.

Analyzer configuration should cover:

- enabled rules
- identifier naming style
- restrictions for built-in calls such as `println`

## Swappable per-version behavior

`NamingStyleRules` decides what pattern each `NamingStyle` value (`SNAKE_CASE`, `CAMEL_CASE`)
matches. It is constructor-injected into `StaticAnalyzer` (default `v1()`), selected by the
composition root in [toolchain](../toolchain/ARCHITECTURE.md), instead of being a hardcoded
`switch` over a fixed set of regexes.

Representative tests: `src/test/java/org/printscript/analyzer/StaticAnalyzerTest.java` (naming and
`println`/`readInput` argument-shape rules), `StaticAnalyzerV11Test.java`.
