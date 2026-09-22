# Semantics Module

The semantics module owns language meaning and correctness.

Responsibilities:

- symbol resolution
- type checking
- built-in function resolution
- semantic validation
- shared semantic rules

Type errors, undeclared variables, invalid assignments, and invalid built-in calls belong here.

Style and policy checks belong in the analyzer module.

`println` should be parsed as a call by syntax and resolved as a built-in function by semantics.

Built-ins should be handled through a small registry from the start. The registry maps a callable name to its signature and behavior contract.

For version `1.0.0`, the registry only needs `println`, but the model should support more built-ins and future user-defined functions.

Variable declarations require explicit type annotations.

## Swappable per-version behavior

Version-specific literal behavior lives behind small strategy interfaces (default `v1()`
implementation, constructor-injected into `SemanticContext`/`SemanticModelBuilder`, selected by the
composition root in `application`) rather than hardcoded `switch`/`if` chains:

- `TypeAnnotationTable` — resolves a type-annotation lexeme (e.g. `"number"`, `"string"`) to a
  `TypeName`.
- `BinaryOperatorRules` — the single source of truth for what result type (if any) a binary
  operator produces given two operand types. This is also what
  [`interpreter.Interpreter`](../interpreter/ARCHITECTURE.md) relies on indirectly: the interpreter
  does not re-derive "is `+` string concatenation or numeric addition" from runtime values, it reads
  the type this rule already assigned during validation (`SemanticModel.typeOf`). Keep that rule
  here, not duplicated at the interpreter level — the interpreter should only ever *act on* a type
  decision, never *make* one.

Representative tests: `src/test/java/org/printscript/semantics/SemanticContextV11Test.java`
(the v1.1 builtins/const/if rules), `SemanticContextDeclarationTest.java` (declaration/type-checking
rules), `BinaryOperatorRulesTest.java`.
