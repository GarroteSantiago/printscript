# CLI Module

The `cli` module is the CLI adapter — the only module allowed to depend on
[Toolchain Module](../toolchain/ARCHITECTURE.md). See the [root README](../README.md) and
[docs/architecture.md](../docs/architecture.md) for how this fits into the rest of the system.

The CLI should only handle:

- argument parsing (`org.example.cli.App`, built on picocli)
- file, stdin, and stdout wiring
- command selection
- JSON config loading
- terminal-friendly progress rendering
- terminal-friendly diagnostic rendering

It should not contain language logic.

Commands (see `App.ExecuteCommand`/`FormatCommand`/`AnalyzeCommand`/`ValidateCommand`):

- `execute`: validate and run source
- `format`: rewrite source formatting through the formatter
- `analyze`: run semantic validation plus analyzer rules
- `validate`: parse and run semantic validation only

Configuration files use JSON. The CLI owns JSON loading (`toolchain.JsonPrintScriptConfigReader`,
selected here and injected behind the `toolchain.PrintScriptConfigReader` port) and converts
configuration files into immutable core config objects (`formatter.FormatterConfig`,
`analyzer.AnalyzerConfig`). See `JsonPrintScriptConfigReader`'s Javadoc for the exact key schema.

Progress is reported through `toolchain.ProgressReporter`, and `readInput`/`readEnv` reach real
stdin/the process environment only through `toolchain.InputSource`/`EnvironmentSource` — both
constructed in `App` and nowhere else — so the core remains independent from the CLI and from any
concrete I/O. These are `toolchain`'s own port types, not `interpreter`'s — `PrintScript` adapts
them internally when it constructs the `Interpreter` that actually needs them, so `cli` depends on
nothing below `toolchain` except `diagnostics` (needed to read `CommandResult`'s `Diagnostic`
values back out for terminal rendering).

Representative test: `src/test/java/org/example/cli/AppTest.java`.
