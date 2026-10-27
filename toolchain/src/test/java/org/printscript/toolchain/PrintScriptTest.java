package org.printscript.toolchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.interpreter.RuntimeEnvironment;
import org.printscript.testkit.TestConfigs;

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
    CommandResult<List<String>> result = executeVersionOneProgram();

    assertTrue(result.isSuccess(), "expected execution to succeed");
  }

  @Test
  public void executesVersionOneProgramWithExpectedOutput() {
    CommandResult<List<String>> result = executeVersionOneProgram();

    assertEquals(List.of("Joe Doe", "Result: 3"), result.value(), "expected println output lines");
  }

  private CommandResult<List<String>> executeVersionOneProgram() {
    return Commands.execute(VERSION_ONE_SOURCE, LanguageVersion.V1_0_0);
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
            prompt -> "",
            name -> Optional.empty(),
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
            TestConfigs.analyzerDefaults(),
            diagnostics::add,
            ProgressReporter.NONE);
  }

  @Test
  public void executeReportsARuntimeFailureAsADiagnosticFailure() {
    CommandResult<List<String>> result =
        Commands.execute("let a: number = 1 / 0;", LanguageVersion.V1_0_0);

    assertFalse(
        result.isSuccess(), "expected a division-by-zero runtime failure to fail execution");
  }

  @Test
  public void executeReportsARuntimeFailureMessage() {
    CommandResult<List<String>> result =
        Commands.execute("let a: number = 1 / 0;", LanguageVersion.V1_0_0);

    assertEquals(
        "Division by zero",
        result.diagnostics().getFirst().message(),
        "expected the division-by-zero diagnostic message");
  }

  @Test
  public void analyzeStringOverloadCollectsDiagnostics() {
    CommandResult<List<Diagnostic>> result =
        Commands.analyze(
            "let badName: string = \"hello\";",
            LanguageVersion.V1_0_0,
            TestConfigs.analyzerDefaults());

    assertEquals(1, result.value().size(), "expected the collected diagnostics list");
  }
}
