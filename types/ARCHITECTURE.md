# Types Module

The types module owns the type-system vocabulary: the finite set of PrintScript types and how a
type-annotation lexeme (`"number"`, `"string"`, `"boolean"`) resolves to one. It has **no
dependencies** — not even on [tokens](../tokens/ARCHITECTURE.md) — because nothing here needs a
source position, a token, or a tree shape to exist.

Responsibilities:

- `TypeName` — the closed set of PrintScript types (`NUMBER`, `STRING`, `BOOLEAN`), dispatched via
  `accept`/`TypeNameVisitor` rather than `instanceof`, matching the same visitor shape used
  throughout [ast](../ast/ARCHITECTURE.md).
- `TypeAnnotationTable` — a swappable, version-specific strategy (`v1()`, `v1_1()`) that resolves a
  type-annotation lexeme to a `TypeName`. Consumed by [typechecker](../typechecker/ARCHITECTURE.md),
  not by anything in this module.

## Why this is a separate module from `ast`

This module used to live inside `syntax` (now [ast](../ast/ARCHITECTURE.md)), but it isn't AST
shape: a `TypeName` doesn't describe tree structure, it describes a value in the type system, and
`TypeAnnotationTable` doesn't touch a node at all — it maps a `String` lexeme to a `TypeName`. The
one place the AST touches this vocabulary is `LiteralExpressionSyntax.literalType`, which is why
`ast` depends on `types` (`requires transitive`, since that field is part of `ast`'s public API) —
not the other way around. [typetable](../typetable/ARCHITECTURE.md) also depends on `types`
directly (via `ast`'s transitive dependency) to type `VariableSymbol`/`BuiltinSignature`.

Representative tests: `src/test/java/org/printscript/types/TypeNameTest.java`,
`TypeAnnotationTableTest.java`.
