# Typechecker Module

The typechecker module is the sole production implementation of semantic analysis: it turns an AST
statement into a [typetable](../typetable/ARCHITECTURE.md) `SemanticModel`, or a diagnostic
explaining why it couldn't.

Responsibilities:

- `SemanticContext` — immutable symbol table plus the per-statement type checker. `validate` never
  mutates `this`: it returns a `SemanticStatementResult` carrying the *next* context, so a caller
  (`ValidatingStatementSource`) can validate a program statement-by-statement without ever holding a
  context that reflects a statement that failed to validate.
- `SemanticStatementResult` — one validation step's result: the next `SemanticContext`, the
  resulting `SemanticModel`, and any diagnostics.
- `SemanticModelBuilder` — whole-program convenience over `SemanticContext#validate`: folds every
  statement through successive contexts into one merged `SemanticModel`. Not the production path —
  `ValidatingStatementSource` validates one statement at a time, off a `StatementSource`, threading
  the context itself. Used mainly by this module's own tests.
- `BinaryOperatorRules` — the single source of truth for what result type, if any, a binary operator
  produces given its operand types. `interpreter.Interpreter` deliberately does not reimplement this
  decision at runtime; it only acts on the `TypeName` this rule already assigned during validation
  (via `SemanticModel.typeOf`).
- `ValidatingStatementSource` — the sole production implementation of
  `typetable.ValidatedStatementSource`: wraps a raw `ast.StatementSource` and a `SemanticContext`,
  threading the context across pulls internally so no caller manages that state across a loop, and
  throwing `typetable.SemanticException` the moment a statement fails validation.
- `SemanticContext.forVersion(boolean)` — selects the `BuiltinRegistry`/`TypeAnnotationTable`
  strategy pair for a language version, so `toolchain` never needs to import `typetable`/`types`
  itself just to pick one.

Split out from `typetable` (then still named `semantics`) on purpose. `interpreter` and `analyzer`
both need to read a `SemanticModel` — neither one re-derives a type decision or a symbol
resolution — but neither one runs the checker itself; they receive an already-validated model from
`toolchain`'s composition root. Before this split, both modules pulled in
`SemanticContext`/`SemanticModelBuilder`/`BinaryOperatorRules` transitively through that module
even though neither referenced them.
Depending on `typechecker` at all is now a signal that a consumer runs analysis, not just reads its
result; today that's only `toolchain` (composition root) and, for test fixtures that build a model
directly, `interpreter`'s and `analyzer`'s own test source sets.

Type checking, symbol resolution, and built-in call resolution all lean on constructor-injected,
swappable strategies (`TypeAnnotationTable`, `BinaryOperatorRules`, `BuiltinRegistry`) rather than
hardcoded rules, so a new language version is a new strategy selected by the `toolchain`
composition root, not a change to `SemanticContext`'s dispatch logic.

## A visible encapsulation trade-off

`SemanticModel.Builder` (the mutable accumulator `SemanticContext` fills in while validating) is
`public`, even though nothing outside this module should construct one directly — read a
`SemanticModel` through its own accessors instead. It has to be public because `SemanticContext` no
longer shares a package with `SemanticModel` after this split; before the split, package-private
access was enough to keep it internal. This is the one real cost of separating the checker from the
model it produces, and it's deliberate rather than an oversight.

Representative tests: `SemanticContextV11Test.java` (the v1.1 builtins/const/if rules),
`SemanticContextDeclarationTest.java` (declaration/type-checking rules),
`SemanticContextModelAccessorsTest.java` (reading resolved types/variables/calls back off a
`SemanticModel`), `SemanticModelBuilderTest.java`, `BinaryOperatorRulesTest.java`.
