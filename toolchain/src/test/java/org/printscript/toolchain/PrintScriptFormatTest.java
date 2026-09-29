package org.printscript.toolchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.printscript.formatter.FormatterConfig;

class PrintScriptFormatTest {
  private static final String SOURCE = "let text: string = \"hello\";\nprintln(text);";

  @Test
  public void formatsAStringSourceSuccessfully() {
    CommandResult<String> result =
        new PrintScript()
            .format(SOURCE, LanguageVersion.V1_0_0, defaultConfig(), ProgressReporter.NONE);

    assertTrue(result.isSuccess(), "expected formatting to succeed");
  }

  @Test
  public void formatsAStringSourceWithExpectedOutput() {
    CommandResult<String> result =
        new PrintScript()
            .format(SOURCE, LanguageVersion.V1_0_0, defaultConfig(), ProgressReporter.NONE);

    assertEquals(
        "let text: string = \"hello\";\nprintln(text);\n",
        result.value(),
        "expected the formatted source");
  }

  @Test
  public void formatsIntoAnAppendableSinkSuccessfully() throws Exception {
    FormatSinkRun run = runFormatIntoSink();

    assertTrue(run.result().isSuccess(), "expected formatting into the sink to succeed");
  }

  @Test
  public void formatsIntoAnAppendableSinkWithExpectedOutput() throws Exception {
    FormatSinkRun run = runFormatIntoSink();

    assertEquals(
        "let text: string = \"hello\";\nprintln(text);\n",
        run.output().toString(),
        "expected the formatted source written to the sink");
  }

  private FormatSinkRun runFormatIntoSink() throws Exception {
    StringWriter output = new StringWriter();

    CommandResult<Void> result =
        new PrintScript()
            .format(
                new StringReader(SOURCE),
                LanguageVersion.V1_0_0,
                defaultConfig(),
                output,
                ProgressReporter.NONE);

    return new FormatSinkRun(result, output);
  }

  private record FormatSinkRun(CommandResult<Void> result, StringWriter output) {}

  @Test
  public void rejectsAnUnsupportedVersion() {
    CommandResult<String> result = formatWithUnsupportedVersion();

    assertFalse(result.isSuccess(), "expected an unsupported version to fail");
  }

  @Test
  public void rejectsAnUnsupportedVersionWithExpectedDiagnostic() {
    CommandResult<String> result = formatWithUnsupportedVersion();

    assertTrue(
        result.diagnostics().getFirst().message().contains("Unsupported PrintScript version"),
        "expected an unsupported-version diagnostic");
  }

  private CommandResult<String> formatWithUnsupportedVersion() {
    return new PrintScript()
        .format(SOURCE, new LanguageVersion(9, 9, 0), defaultConfig(), ProgressReporter.NONE);
  }

  @Test
  public void reportsASyntaxErrorAsADiagnosticFailure() {
    CommandResult<String> result =
        new PrintScript()
            .format(
                "let x number = 1;",
                LanguageVersion.V1_0_0,
                defaultConfig(),
                ProgressReporter.NONE);

    assertFalse(result.isSuccess(), "expected a syntax error to fail formatting");
  }

  private FormatterConfig defaultConfig() {
    return new FormatterConfig(0, 0, 1, 1, 1, 2);
  }
}
