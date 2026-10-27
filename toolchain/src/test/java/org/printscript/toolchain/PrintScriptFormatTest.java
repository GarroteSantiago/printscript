package org.printscript.toolchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.printscript.formatter.FormatterConfig;
import org.printscript.testkit.TestConfigs;

class PrintScriptFormatTest {
  private static final String SOURCE = "let text: string = \"hello\";\nprintln(text);";

  @Test
  public void formatsAStringSourceSuccessfully() {
    CommandResult<String> result = Commands.format(SOURCE, LanguageVersion.V1_0_0, defaultConfig());

    assertTrue(result.isSuccess(), "expected formatting to succeed");
  }

  @Test
  public void formatsAStringSourceWithExpectedOutput() {
    CommandResult<String> result = Commands.format(SOURCE, LanguageVersion.V1_0_0, defaultConfig());

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
  public void formatsSyntacticallyInvalidCodeWithoutValidatingIt() {
    CommandResult<String> result =
        Commands.format("let x number = 1;", LanguageVersion.V1_0_0, defaultConfig());

    assertEquals(
        "let x number = 1;\n",
        result.value(),
        "expected formatting to work on the tokens alone, leaving validation to validate");
  }

  @Test
  public void reportsALexicalErrorAsADiagnosticFailure() {
    CommandResult<String> result =
        Commands.format("let s: string = \"unterminated;", LanguageVersion.V1_0_0, defaultConfig());

    assertFalse(result.isSuccess(), "expected source that can't be tokenized to fail formatting");
  }

  private FormatterConfig defaultConfig() {
    return TestConfigs.formatterOneBlankLineAfterPrintln();
  }
}
