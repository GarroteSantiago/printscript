# Typetable Module

The typetable module owns the *result* of semantic analysis, not the analysis itself: the record of
decisions a type checker made, and the vocabulary needed to describe them.

Responsibilities:

- `SemanticModel` — the record of decisions made while validating a statement: each expression's
  resolved `TypeName`, each identifier's resolved `VariableSymbol`, each call's resolved
  `BuiltinSignature`. Downstream stages (`interpreter`, `analyzer`) read these decisions instead of
  re-deriving them.
- `BuiltinRegistry` / `BuiltinSignature` — which callables (`println`, `readInput`, `readEnv`) exist
  for a given language version, and their signatures.
- `VariableSymbol` — a declared variable's resolved name, type, mutability, and declaration site.

The actual type checker — `SemanticContext`, the whole-program `SemanticModelBuilder` convenience,
and the `BinaryOperatorRules` strategy that decides operator result types — lives in a separate
[typechecker](../typechecker/ARCHITECTURE.md) module, not here. This module defines what a
completed analysis looks like; it never performs one.

Type errors, undeclared variables, invalid assignments, and invalid built-in calls are decided by
the checker and recorded here. Style and policy checks belong in the analyzer module, on top of an
already-valid `SemanticModel`.

`println` is parsed as a call by `ast` and resolved as a built-in function through
`BuiltinRegistry`. For version `1.0.0` the registry only needs `println`, but the model supports
more built-ins and future user-defined functions.

Variable declarations require explicit type annotations.

## Why this is a separate module from the checker

`interpreter` and `analyzer` both need to read a `SemanticModel` (and know what builtins exist) —
neither one re-derives a type decision or a symbol resolution. Neither one, however, ever runs the
checker itself: they receive an already-validated model from `application`'s composition root. Before
this split, both modules pulled in `SemanticContext`, `SemanticModelBuilder`, and
`BinaryOperatorRules` transitively through this module even though they never referenced them.
Depending on `typechecker` is now a signal that a consumer runs analysis, not just reads its result;
today that's only `application` (the composition root) and, for test fixtures, `interpreter`'s and
`analyzer`'s own test source sets.

This is the same reasoning behind the `tokens`/`lexer` split (contract vs. one implementation), the
`ast`/`parser` split (tree shape vs. the code that builds one), and the `ast`/`types` split (tree
shape vs. type-system vocabulary), applied here a fourth time to the type checker.

## Why this module is called `typetable`, not `semantics`

The module used to be named `semantics`, back when it held the whole semantic-analysis story. Once
the checker moved out to `typechecker`, that name overpromised: what's left is one specific,
immutable data structure — an AST-node-identity-keyed table of resolved types, symbols, and call
signatures — not "the semantics of the language." `typetable` says what's actually in the jar.

Representative tests: `src/test/java/org/printscript/typetable/BuiltinSignatureTest.java`.
Checker-specific tests (`SemanticContextV11Test.java`, `SemanticContextDeclarationTest.java`,
`SemanticContextModelAccessorsTest.java`, `SemanticModelBuilderTest.java`,
`BinaryOperatorRulesTest.java`) live in [typechecker](../typechecker/ARCHITECTURE.md) alongside the
code they test.
