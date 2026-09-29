package org.printscript.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ReplTest {
  private record RunResult(String out, String err) {}

  private RunResult run(String source, boolean v11) {
    ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

    Repl.run(
        new StringReader(source),
        v11,
        new PrintStream(outBuffer, true, StandardCharsets.UTF_8),
        new PrintStream(errBuffer, true, StandardCharsets.UTF_8));

    return new RunResult(
        outBuffer.toString(StandardCharsets.UTF_8), errBuffer.toString(StandardCharsets.UTF_8));
  }

  private RunResult runValidProgram() {
    return run("let x: number = 1; println(x);", false);
  }

  @Test
  public void printsOnePromptBeforeEachStatementPlusATrailingOne() {
    assertEquals(
        3,
        runValidProgram().out().split("ps> ", -1).length - 1,
        "expected one prompt before each statement, plus a trailing one");
  }

  @Test
  public void printlnOutputAppearsOnStdout() {
    assertTrue(
        runValidProgram().out().contains("1"), "expected println(x) to have written its value");
  }

  @Test
  public void validInputProducesNoDiagnostics() {
    assertEquals("", runValidProgram().err(), "expected no diagnostics for valid input");
  }

  private RunResult runProgramWithASemanticError() {
    return run("let x: string = 1; println(\"still alive\");", false);
  }

  @Test
  public void semanticErrorIsRenderedOnStderr() {
    RunResult result = runProgramWithASemanticError();
    assertTrue(
        result.err().contains("SEMANTIC"),
        "expected a rendered semantic diagnostic on stderr: " + result.err());
  }

  @Test
  public void theStatementAfterASemanticErrorStillRuns() {
    assertTrue(
        runProgramWithASemanticError().out().contains("still alive"),
        "expected the statement after the error to still run");
  }

  @Test
  public void v11BlockStatementSpanningMultipleReaderLinesExecutes() {
    RunResult result = run("if (true) {\n  println(\"block\");\n}\n", true);

    assertTrue(
        result.out().contains("block"),
        "expected a block statement spanning several lines of the reader to execute");
  }

  private RunResult runProgramWithAStandaloneUnrecognizedCharacter() {
    return run("let x: number = 1;\n@\nprintln(\"after\");\n", false);
  }

  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  public void aStandaloneUnrecognizedCharacterIsReportedAsADiagnostic() {
    assertTrue(
        runProgramWithAStandaloneUnrecognizedCharacter().err().contains("Unexpected character"),
        "expected the bad character to be reported, not silently swallowed");
  }

  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  public void theStatementAfterAStandaloneUnrecognizedCharacterStillRuns() {
    assertTrue(
        runProgramWithAStandaloneUnrecognizedCharacter().out().contains("after"),
        "expected the next statement, on its own line after the bad character, to still run");
  }

  /**
   * A syntax error embedded inside a multi-token compound statement (here, a `>` the grammar
   * doesn't support, inside an `if` condition) is a much harder case: recovering from it can cost
   * several subsequent tokens — occasionally an entire following statement — before the read loop
   * resynchronizes, because {@code StatementSyntaxReader}'s already-fetched lookahead is lost on
   * every failed resync attempt and there is no way to ask it to back up rather than discard. What
   * is guaranteed regardless is that the loop always terminates rather than hanging, which is what
   * this asserts via a timeout; see {@link Repl}'s class doc for why this is not a full recovery
   * guarantee.
   */
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  public void aSyntaxErrorInsideAMultiTokenStatementNeverHangsTheSession() {
    RunResult result =
        run("let a: number = 7;\nif (a > 3) { println(\"x\"); }\nprintln(\"done\");\n", true);

    assertTrue(
        result.err().contains("Unexpected character"),
        "expected the malformed condition to be reported: " + result.err());
  }
}
