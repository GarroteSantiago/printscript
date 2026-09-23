# PrintScript

PrintScript is a small statically-typed scripting language, plus a toolchain for it: a lexer,
parser, semantic analyzer, interpreter, source formatter, and static analyzer, wired together
behind a single facade and exposed today through a command-line adapter. Two language versions
exist side by side, `1.0.0` and `1.1.0` (`1.1` adds `const`, `if`/`else`, a `boolean` type, and the
`readInput`/`readEnv` builtins on top of `1.0`) — see [`LanguageVersion`](application/src/main/java/org/printscript/application/LanguageVersion.java).

This is a Gradle multi-module Java project. The build enforces Checkstyle, PMD, Spotless formatting,
and 80% JaCoCo test coverage on every module (see [`justfile`](justfile) and
[`.github/workflows/ci.yml`](.github/workflows/ci.yml)).

## Where to start reading

- **The composition root**: [`application/PrintScript.java`](application/src/main/java/org/printscript/application/PrintScript.java)
  wires a requested `LanguageVersion` into a concrete lexer/parser/semantic/interpreter/formatter/
  analyzer pipeline and exposes `execute`/`format`/`analyze`/`validate`. Nearly every other class in
  the core exists to be selected or called from here.
- **The CLI entry point**: [`cli/App.java`](cli/src/main/java/org/example/cli/App.java) — argument
  parsing and I/O wiring around the same four operations.
- **The full architecture write-up, with diagrams**: [`docs/architecture.md`](docs/architecture.md).

## Modules

Each module has its own `ARCHITECTURE.md` — responsibilities, dependencies, design rules, and
pointers to the classes and tests worth reading first. Arrows below are the real Gradle dependency
direction; the full graph (and why `lexer`/`parser` don't depend on each other) is in
[`docs/architecture.md`](docs/architecture.md#module-dependency-graph).

| Module | Responsibility |
|---|---|
| [`source`](source/ARCHITECTURE.md) | Source-position/span value types. Zero dependencies. |
| [`diagnostics`](diagnostics/ARCHITECTURE.md) | Shared vocabulary for reporting problems in user code (`Diagnostic`, `Severity`, `Phase`). |
| [`tokens`](tokens/ARCHITECTURE.md) | The token-streaming contract (`Token`, `TokenType`, `TokenSource`, `SyntaxException`) shared by `lexer` and `parser` without either depending on the other. |
| [`lexer`](lexer/ARCHITECTURE.md) | Scans source text into tokens (`Lexer`). |
| [`types`](types/ARCHITECTURE.md) | The type-system vocabulary (`TypeName`, `TypeAnnotationTable`). Zero dependencies. |
| [`ast`](ast/ARCHITECTURE.md) | Owns the AST node types and the `StatementSource` port a parser implements. |
| [`parser`](parser/ARCHITECTURE.md) | Parses tokens into an AST (`StatementSyntaxReader`, `SyntaxTreeBuilder`) — the only production implementation of `StatementSource`. |
| [`typetable`](typetable/ARCHITECTURE.md) | The result of semantic analysis (`SemanticModel`) and its vocabulary (`BuiltinRegistry`, `VariableSymbol`) — not the checker itself. |
| [`typechecker`](typechecker/ARCHITECTURE.md) | Type checking, symbol resolution, builtin resolution (`SemanticContext`) — the sole production producer of a `SemanticModel`. |
| [`interpreter`](interpreter/ARCHITECTURE.md) | Executes validated statements (`Interpreter`), immutable runtime state. |
| [`formatter`](formatter/ARCHITECTURE.md) | Lossless, trivia-based source rewriting (`PrintScriptFormatter`). |
| [`analyzer`](analyzer/ARCHITECTURE.md) | Configurable style/policy checks on top of an already-valid program (`StaticAnalyzer`). |
| [`application`](application/ARCHITECTURE.md) | The composition root and public facade (`PrintScript`), CLI-independent. |
| [`cli`](cli/ARCHITECTURE.md) | The command-line adapter (`App`) — the only module allowed to depend on `application`. |
| [`testkit`](testkit/ARCHITECTURE.md) | Test-only helper for turning a string into pipeline data (`TestSources`). |

## Building, testing, running

Common tasks are wrapped as [`just`](https://github.com/casey/just) recipes (see the
[`justfile`](justfile) for the full list); each just calls `./gradlew` directly if you don't have
`just` installed.

```sh
just build            # compile everything (skip tests/analysis)
just test             # run all tests
just coverage         # run tests + JaCoCo report, enforce the 80% threshold
just lint             # Checkstyle + PMD + Spotless check
just format           # apply Spotless formatting
just check            # everything CI runs, before pushing

# Run the CLI directly:
just run "execute --source=path/to/file.pisp --version=1.0"
just run "format --source=path/to/file.pisp --version=1.0"
just run "analyze --source=path/to/file.pisp --version=1.0 --config=path/to/config.json"
just run "validate --source=path/to/file.pisp --version=1.0"
```

`format`/`analyze` take a JSON config file — see
[`JsonPrintScriptConfigReader`](application/src/main/java/org/printscript/application/JsonPrintScriptConfigReader.java)'s
Javadoc for the recognized keys.

## Further reading

- [`docs/architecture.md`](docs/architecture.md) — module dependency graph, pipeline data flow, and
  the recurring visitor-pattern convention used across the core, plus the project's design rules.
- [`docs/findings-review.md`](docs/findings-review.md) — known, not-yet-fixed inconsistencies
  between some modules' `build.gradle`/`module-info.java` descriptors.
