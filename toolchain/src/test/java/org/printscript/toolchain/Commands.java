package org.printscript.toolchain;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.formatter.FormatterConfigProvider;

/**
 * Test helper: runs each {@link PrintScript} command on a source string through the same overload
 * the CLI calls, collecting what the CLI would print into a value. {@code readInput}/{@code
 * readEnv} are unavailable here; tests that need them call {@link PrintScript} directly.
 */
final class Commands {
  private static final InputSource NO_INPUT =
      prompt -> {
        throw new UnsupportedOperationException("readInput is not available in this test");
      };
  private static final EnvironmentSource NO_ENVIRONMENT = name -> Optional.empty();

  private Commands() {}

  /** The lines {@code execute} prints, or its diagnostics. */
  static CommandResult<List<String>> execute(String source, LanguageVersion version) {
    List<String> output = new ArrayList<>();
    CommandResult<?> result =
        new PrintScript()
            .execute(
                new StringReader(source),
                version,
                output::add,
                NO_INPUT,
                NO_ENVIRONMENT,
                ProgressReporter.NONE);
    return result.isSuccess()
        ? CommandResult.success(output)
        : CommandResult.failure(result.diagnostics());
  }

  /** The formatted source, or the diagnostics that stopped formatting. */
  static CommandResult<String> format(
      String source, LanguageVersion version, FormatterConfigProvider config) {
    StringBuilder output = new StringBuilder();
    CommandResult<Void> result;
    try {
      result =
          new PrintScript()
              .format(new StringReader(source), version, config, output, ProgressReporter.NONE);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return result.isSuccess()
        ? CommandResult.success(output.toString())
        : CommandResult.failure(result.diagnostics());
  }

  /** Every analyzer diagnostic, or the diagnostics that stopped analysis. */
  static CommandResult<List<Diagnostic>> analyze(
      String source, LanguageVersion version, AnalyzerConfig config) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    CommandResult<AnalysisResult> result =
        new PrintScript()
            .analyze(
                new StringReader(source), version, config, diagnostics::add, ProgressReporter.NONE);
    return result.isSuccess()
        ? CommandResult.success(diagnostics)
        : CommandResult.failure(result.diagnostics());
  }

  static CommandResult<Void> validate(String source, LanguageVersion version) {
    return new PrintScript().validate(new StringReader(source), version, ProgressReporter.NONE);
  }
}
