# Findings from architecture review (2026-09-08)

Concrete, actionable fixes found while reviewing `application/PrintScript.java`
and `interpreter/Interpreter.java`. Design tradeoffs that were discussed and
kept as-is (e.g. single AST/CST tree, version dispatch in the composition
root) are not listed here — only items with a clear fix.

## 1. Duplicated pipeline wiring in `PrintScript.java`

`new StatementSyntaxReader(new Lexer(source, pipeline.keywords()))` is
repeated verbatim in `execute`, `format`, `analyze`, and `validate`
(`application/src/main/java/org/printscript/application/PrintScript.java:114,168,211,253`).
Same knowledge, same reason to change, copy-pasted 4 times.

**Fix**: extract a private helper on `PrintScript`:

```java
private StatementSource statementsFor(Reader source, LanguagePipeline pipeline) {
  return new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
}
```

and call it from all four call sites. Keep it private/local to `PrintScript`
— no other module needs it yet.

## 2. Unwrapped `UnsupportedOperationException` breaks the `CommandResult` contract

`Interpreter`'s 2-arg constructor (`Interpreter(OutputPort, ArithmeticOperators)`,
used by `PrintScript.execute(Reader, LanguageVersion, ProgressReporter)` and the
`format`/`analyze`/`validate` no-I/O paths) defaults `input`/`env` to:

```java
// interpreter/src/main/java/org/printscript/interpreter/Interpreter.java:43-53
private static InputPort unsupportedInput() {
  return prompt -> {
    throw new UnsupportedOperationException("readInput is not available in this context");
  };
}

private static EnvironmentPort unsupportedEnvironment() {
  return name -> {
    throw new UnsupportedOperationException("readEnv is not available in this context");
  };
}
```

`readInput`/`readEnv` call these directly with no local try/catch
(`Interpreter.java:174-178`). Every other public method on `PrintScript`
(`execute`, `format`, `analyze`, `validate`) always returns
`CommandResult<T>` and never lets an exception escape — `RuntimeFailure` and
`SyntaxException` are caught and converted to `CommandResult.failure(...)`
(`PrintScript.java:124-128`). This fourth error path (unsupported I/O in the
current execution context) is the odd one out: it throws an unchecked
`UnsupportedOperationException` straight through the facade instead of
surfacing as a structured `Diagnostic`, which violates the project's own
design rule ("Surface user-code problems as structured diagnostics",
`docs/architecture.md`).

**Fix**: make the defaults raise the same failure type the rest of the
runtime path already uses, so it's caught by the existing
`catch (RuntimeFailure failure)` in `PrintScript.execute`:

```java
private static InputPort unsupportedInput() {
  return prompt -> {
    throw new RuntimeFailure(Diagnostic.error(Phase.RUNTIME,
        "readInput is not available in this context", /* span */));
  };
}
```

Note `RuntimeFailure`'s diagnostic normally carries the call-site
`SourceSpan` (see `Interpreter.runtime(String, ExpressionSyntax)` at
`Interpreter.java:224-225`) — the port lambdas don't have access to the
`CallExpressionSyntax` today, so the call sites at `readInput`/`readEnv`
(`Interpreter.java:174-178`) should catch `UnsupportedOperationException`
locally and rethrow as `RuntimeFailure` with the expression's span, rather
than changing `unsupportedInput()`/`unsupportedEnvironment()` themselves.

## 3. Module name typo: `org.prinstscript.application`

`application/src/main/java/module-info.java` declares:

```java
module org.prinstscript.application {   // "prinstscript", missing the 'e' in "print"
```

and `cli/src/main/java/module-info.java` requires it under the same
misspelled name (`requires org.prinstscript.application;`). Every other
module in the project follows `org.printscript.<module>`. It compiles fine
today because the typo is consistent between declarer and the one consumer,
but it's a landmine: the day a second module adds
`requires org.printscript.application;` (the correctly-spelled, expected
name, matching the pattern every other module follows), the build fails
with a module-not-found error that's non-obvious to root-cause.

**Fix**: rename the module to `org.printscript.application` in both
`application/src/main/java/module-info.java` and the `requires` line in
`cli/src/main/java/module-info.java`.

## 4. `application`'s module-info doesn't mirror Gradle's `api` transitivity

`application/build.gradle` declares `formatter` and `analyzer` as `api
project(...)` (meaning Gradle re-exposes their classes to whoever depends on
`application`, i.e. `cli`), while `syntax`, `semantics`, `interpreter`, and
`lexer` are `implementation` (not re-exposed). But
`application/src/main/java/module-info.java` uses a plain `requires` for
*all* of them — none are `requires transitive`. So at the Gradle/classpath
level, `cli` can reach `formatter`/`analyzer` types transitively; at the
JPMS/module level, it cannot (matches what `cli`'s own `module-info.java`
already has to work around by explicitly `requires`-ing `diagnostics` and
`interpreter` directly, since `application` doesn't re-export those either).
Two sources of truth for the same "does this get re-exposed" decision,
currently disagreeing for `formatter`/`analyzer`.

**Fix**: either add `requires transitive org.printscript.formatter;` and
`requires transitive org.printscript.analyzer;` to `application`'s
module-info to match the `api` declarations in `build.gradle`, or (if the
intent is that `cli` should always declare what it directly imports,
regardless of what `application` re-exports) downgrade `formatter` and
`analyzer` from `api` to `implementation` in `application/build.gradle` so
both descriptors agree that nothing is transitively re-exposed. Pick
whichever matches the actual intent — right now the two build
descriptions of the same module contradict each other.
