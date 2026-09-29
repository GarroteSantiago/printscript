package org.printscript.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

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
}
