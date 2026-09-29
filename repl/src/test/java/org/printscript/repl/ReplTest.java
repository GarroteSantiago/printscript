package org.printscript.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
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
  public void printsOnePromptBeforeTheLineAndOneAfterItRuns() {
    assertEquals(
        2,
        runValidProgram().out().split("ps> ", -1).length - 1,
        "expected one prompt before the line (both statements share it, one line of input) plus"
            + " one after it finishes running");
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
   * doesn't support, inside an `if` condition) recovers just as cleanly as a standalone bad token:
   * each line's buffer is parsed once, in full, over an already-complete in-memory source, so a
   * failure discards that one buffer atomically rather than leaving any parser lookahead to lose
   * tokens from on a retry. The {@code @Timeout} guards against ever regressing back to the hang a
   * streaming, incrementally-resynced parser was prone to (see {@link Repl}'s class doc).
   */
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  public void aSyntaxErrorInsideAMultiTokenStatementIsReported() {
    RunResult result =
        run("let a: number = 7;\nif (a > 3) { println(\"x\"); }\nprintln(\"done\");\n", true);

    assertTrue(
        result.err().contains("Unexpected character"),
        "expected the malformed condition to be reported: " + result.err());
  }

  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  public void theStatementAfterAMultiTokenSyntaxErrorStillRunsCleanly() {
    RunResult result =
        run("let a: number = 7;\nif (a > 3) { println(\"x\"); }\nprintln(\"done\");\n", true);

    assertTrue(
        result.out().contains("done"),
        "expected the next line's statement to run normally, with no tokens lost: " + result.out());
  }

  /**
   * A regression test built on a fully-materialized {@link StringReader} cannot catch a prompt
   * appearing late: {@link Repl#run} only returns once the whole source is exhausted, by which
   * point every prompt has eventually printed regardless of when it appeared relative to the input
   * that produced it. This test instead feeds input incrementally through a real {@link
   * PipedWriter}, on a background thread, so it can assert the second prompt is already on stdout
   * strictly *before* the second line is written — catching exactly the bug a user found by hand:
   * every prompt and every statement's output landing one command late, because completing
   * statement N's parse always required blocking to read statement N+1's first token.
   */
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  public void theSecondPromptAppearsBeforeTheSecondLineIsTyped()
      throws IOException, InterruptedException {
    try (PipedWriter typedInput = new PipedWriter();
        PipedReader replInput = new PipedReader(typedInput)) {
      ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
      PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
      PrintStream err = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

      Thread replThread = new Thread(() -> Repl.run(replInput, false, out, err));
      replThread.setDaemon(true);
      replThread.start();

      typedInput.write("let x: number = 1;\n");
      typedInput.flush();

      String expected = "ps> ps> ";
      long deadline = System.currentTimeMillis() + 5000;
      while (!expected.equals(outBuffer.toString(StandardCharsets.UTF_8))
          && System.currentTimeMillis() < deadline) {
        Thread.sleep(20);
      }

      assertEquals(
          expected,
          outBuffer.toString(StandardCharsets.UTF_8),
          "expected the prompt for the next line to already be printed, before that line is"
              + " typed");
    }
  }
}
