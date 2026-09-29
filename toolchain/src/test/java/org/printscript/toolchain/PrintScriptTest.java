package org.printscript.toolchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.interpreter.RuntimeEnvironment;

class PrintScriptTest {
  private static final String VERSION_ONE_SOURCE =
      """
          let name: string = "Joe";
          let lastName: string = 'Doe';
          println(name + " " + lastName);
          let a: number = 12;
          let b: number = 4;
          let c: number = a / b;
          println("Result: " + c);
          """;

  @Test
  public void executesVersionOneProgramSuccessfully() {
    CommandResult<ExecutionResult> result = executeVersionOneProgram();

    assertTrue(result.isSuccess(), "expected execution to succeed");
  }

  @Test
  public void executesVersionOneProgramWithExpectedOutput() {
    CommandResult<ExecutionResult> result = executeVersionOneProgram();

    assertEquals(
        List.of("Joe Doe", "Result: 3"),
        result.value().outputLines(),
        "expected println output lines");
  }

  private CommandResult<ExecutionResult> executeVersionOneProgram() {
    return new PrintScript()
        .execute(VERSION_ONE_SOURCE, LanguageVersion.V1_0_0, ProgressReporter.NONE);
  }

  @Test
  public void executesIntoInjectedOutputPortSuccessfully() {
    List<String> output = new ArrayList<>();

    CommandResult<RuntimeEnvironment> result = executeSampleProgramIntoOutputPort(output);

    assertTrue(result.isSuccess(), "expected execution to succeed");
  }

  @Test
  public void executesIntoInjectedOutputPortWithExpectedOutput() {
    List<String> output = new ArrayList<>();

    executeSampleProgramIntoOutputPort(output);

    assertEquals(List.of("value: 2"), output, "expected println output collected via output port");
  }

  private CommandResult<RuntimeEnvironment> executeSampleProgramIntoOutputPort(
      List<String> output) {
    return new PrintScript()
        .execute(
            new StringReader(
                """
                    let value: number = 2;
                    println("value: " + value);
                    """),
            LanguageVersion.V1_0_0,
            output::add,
            ProgressReporter.NONE);
  }

  @Test
  public void streamsAnalyzerDiagnosticsSuccessfully() {
    List<Diagnostic> diagnostics = new ArrayList<>();

    CommandResult<AnalysisResult> result = analyzeSampleProgram(diagnostics);

    assertTrue(result.isSuccess(), "expected analysis to succeed");
  }

  @Test
  public void streamsAnalyzerDiagnosticsWithExpectedCount() {
    List<Diagnostic> diagnostics = new ArrayList<>();

    CommandResult<AnalysisResult> result = analyzeSampleProgram(diagnostics);

    assertEquals(1, result.value().diagnosticCount(), "expected one diagnostic reported");
  }

  @Test
  public void streamsAnalyzerDiagnosticsWithExpectedMessage() {
    List<Diagnostic> diagnostics = new ArrayList<>();

    analyzeSampleProgram(diagnostics);

    assertEquals(
        "Identifier 'badName' does not match SNAKE_CASE",
        diagnostics.getFirst().message(),
        "expected naming style diagnostic message");
  }

  private CommandResult<AnalysisResult> analyzeSampleProgram(List<Diagnostic> diagnostics) {
    return new PrintScript()
        .analyze(
            new StringReader("let badName: string = \"hello\";"),
            LanguageVersion.V1_0_0,
            AnalyzerConfig.defaults(),
            diagnostics::add,
            ProgressReporter.NONE);
  }

  @Test
  public void executeRejectsAnUnsupportedVersion() {
    CommandResult<ExecutionResult> result = executeWithUnsupportedVersion();

    assertFalse(result.isSuccess(), "expected an unsupported version to fail execution");
  }

  @Test
  public void executeRejectsAnUnsupportedVersionWithExpectedDiagnostic() {
    CommandResult<ExecutionResult> result = executeWithUnsupportedVersion();

    assertTrue(
        result.diagnostics().getFirst().message().contains("Unsupported PrintScript version"),
        "expected an unsupported-version diagnostic");
  }

  private CommandResult<ExecutionResult> executeWithUnsupportedVersion() {
    return new PrintScript()
        .execute("println(\"hi\");", new LanguageVersion(9, 9, 0), ProgressReporter.NONE);
  }

  @Test
  public void executeReportsARuntimeFailureAsADiagnosticFailure() {
    CommandResult<ExecutionResult> result =
        new PrintScript()
            .execute("let a: number = 1 / 0;", LanguageVersion.V1_0_0, ProgressReporter.NONE);

    assertFalse(
        result.isSuccess(), "expected a division-by-zero runtime failure to fail execution");
  }

  @Test
  public void executeReportsARuntimeFailureMessage() {
    CommandResult<ExecutionResult> result =
        new PrintScript()
            .execute("let a: number = 1 / 0;", LanguageVersion.V1_0_0, ProgressReporter.NONE);

    assertEquals(
        "Division by zero",
        result.diagnostics().getFirst().message(),
        "expected the division-by-zero diagnostic message");
  }

  @Test
  public void analyzeStringOverloadCollectsDiagnostics() {
    CommandResult<List<Diagnostic>> result =
        new PrintScript()
            .analyze(
                "let badName: string = \"hello\";",
                LanguageVersion.V1_0_0,
                AnalyzerConfig.defaults(),
                ProgressReporter.NONE);

    assertEquals(1, result.value().size(), "expected the collected diagnostics list");
  }

  @Test
  public void analyzeStringOverloadRejectsAnUnsupportedVersion() {
    CommandResult<List<Diagnostic>> result =
        new PrintScript()
            .analyze(
                "println(\"hi\");",
                new LanguageVersion(9, 9, 0),
                AnalyzerConfig.defaults(),
                ProgressReporter.NONE);

    assertFalse(result.isSuccess(), "expected an unsupported version to fail analysis");
  }
}
