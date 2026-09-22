# Interpreter Module

The interpreter executes validated statements.

Responsibilities:

- statement execution
- expression evaluation
- runtime values
- immutable runtime environment
- output port integration
- runtime diagnostics

Execution should follow this flow:

```text
parse statement
  -> validate statement
  -> execute statement
  -> continue
```

Runtime state should be immutable. Assignment returns a new environment with the updated binding.

This means PrintScript variables may still be reassigned, but the interpreter state object is replaced instead of mutated in place.

Runtime errors should stop execution immediately.

Numbers should use decimal semantics to avoid floating-point surprises.

Callable statements should return values. When no meaningful value exists, the result should be equivalent to Kotlin's `Unit`.

`println` returns the unit value after writing to the output port.

## Swappable per-version behavior

Version-specific literal behavior is abstracted behind small strategy interfaces rather than
hardcoded in `switch` statements, so a future language version can swap them in without touching
this module's dispatch logic:

- `ArithmeticOperators` — what a binary operator (`PLUS`/`MINUS`/`STAR`/`SLASH`) computes given two
  `BigDecimal` operands. `Interpreter` takes one via constructor injection (default `v1()`); the
  composition root in `application` is where a real swap would happen.

`Interpreter.evaluateBinary` does **not** independently decide whether `+` means numeric addition
or string concatenation by inspecting runtime values. That decision was already made once, during
semantic validation, and recorded in the `SemanticModel` passed in — the interpreter just reads
`semanticModel.typeOf(binary)`. This is deliberate: the type-compatibility rule (`+` also valid for
strings, all four operators valid for numbers) lives in exactly one place —
`semantics.BinaryOperatorRules` — instead of being re-implemented here as a duplicate runtime
`instanceof` check. If you're tempted to add a runtime type check to decide operator behavior here,
that logic almost certainly belongs in `BinaryOperatorRules` instead, consulted once during
validation.

Representative tests: `src/test/java/org/printscript/interpreter/InterpreterV11Test.java`
(const/if/boolean/readInput/readEnv behavior), `InterpreterRuntimeFailureTest.java` (division by
zero, unset env vars, malformed `readInput`/`readEnv` input all surfacing as `RuntimeFailure`).
