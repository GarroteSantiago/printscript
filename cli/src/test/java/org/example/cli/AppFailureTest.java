package org.example.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Commands that fail on the user's code, and the real stdin prompt for a missing config file. */
class AppFailureTest {
  private static final String CLOSE_RESOURCE_SUPPRESSION = "PMD.CloseResource";
  private static final String EXPECTED_FAILURE_EXIT_CODE = "expected exit code 1";
  private static final String SOURCE_FLAG = "--source";
  private static final String VERSION_FLAG = "--version";
  private static final String CONFIG_FLAG = "--config";
  private static final String VERSION_ONE_ZERO = "1.0";

  @TempDir Path tempDir;

  /** Runs the CLI exactly as {@code App.main} does, returning the exit code instead of exiting. */
  private static int execute(App app, String... args) {
    return App.commandLine(app).execute(args);
  }

  /** Runs the CLI with {@code stdin} as standard input, discarding stdout and stderr. */
  private record Run(int exitCode, String stderr) {}

  private int run(String stdin, String... args) throws Exception {
    return runCapturingStderr(stdin, args).exitCode();
  }

  /** Runs the CLI with {@code stdin} as standard input, discarding stdout and capturing stderr. */
  @SuppressWarnings(CLOSE_RESOURCE_SUPPRESSION) // the saved streams are restored, not ours to close
  private Run runCapturingStderr(String stdin, String... args) throws Exception {
    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    try (PrintStream discard =
            new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        PrintStream capturedErr = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
      System.setIn(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));
      System.setOut(discard);
      System.setErr(capturedErr);
      int exitCode = execute(new App(), args);
      return new Run(exitCode, stderr.toString(StandardCharsets.UTF_8));
    } finally {
      System.setIn(originalIn);
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  private String source(String code) throws Exception {
    Path source = tempDir.resolve("source.pisp");
    Files.writeString(source, code);
    return source.toString();
  }

  private String config() throws Exception {
    Path config = tempDir.resolve("config.json");
    Files.writeString(config, "{}\n");
    return config.toString();
  }

  @Test
  void executeReturnsOneForASemanticError() throws Exception {
    assertEquals(
        1,
        run(
            "",
            "execute",
            SOURCE_FLAG,
            source("let x: number = \"a\";"),
            VERSION_FLAG,
            VERSION_ONE_ZERO),
        EXPECTED_FAILURE_EXIT_CODE);
  }

  @Test
  void validateReturnsOneForASemanticError() throws Exception {
    assertEquals(
        1,
        run(
            "",
            "validate",
            SOURCE_FLAG,
            source("println(missing);"),
            VERSION_FLAG,
            VERSION_ONE_ZERO),
        EXPECTED_FAILURE_EXIT_CODE);
  }

  @Test
  void formatReturnsOneForALexicalError() throws Exception {
    assertEquals(
        1,
        run(
            "",
            "format",
            SOURCE_FLAG,
            source("let s: string = \"unterminated;"),
            VERSION_FLAG,
            VERSION_ONE_ZERO,
            CONFIG_FLAG,
            config()),
        EXPECTED_FAILURE_EXIT_CODE);
  }

  @Test
  void analyzeReturnsOneForASyntaxError() throws Exception {
    assertEquals(
        1,
        run(
            "",
            "analyze",
            SOURCE_FLAG,
            source("let x number = 1;"),
            VERSION_FLAG,
            VERSION_ONE_ZERO,
            CONFIG_FLAG,
            config()),
        EXPECTED_FAILURE_EXIT_CODE);
  }

  private int formatPromptingForConfig(String stdin) throws Exception {
    return run(stdin, "format", SOURCE_FLAG, source("println(1);"), VERSION_FLAG, VERSION_ONE_ZERO);
  }

  @Test
  void aConfigPathTypedAtThePromptIsUsed() throws Exception {
    assertEquals(0, formatPromptingForConfig(config() + "\n"), "expected the typed path to work");
  }

  @Test
  void noInputAtTheConfigPromptFails() throws Exception {
    assertNotEquals(0, formatPromptingForConfig(""), "expected end of input to be rejected");
  }

  @Test
  void aBlankLineAtTheConfigPromptFails() throws Exception {
    assertNotEquals(0, formatPromptingForConfig("   \n"), "expected a blank path to be rejected");
  }

  private Run formatWithABlankConfigAnswer() throws Exception {
    return runCapturingStderr(
        "   \n", "format", SOURCE_FLAG, source("println(1);"), VERSION_FLAG, VERSION_ONE_ZERO);
  }

  @Test
  void aBlankConfigAnswerIsReportedOnOneLine() throws Exception {
    assertEquals(
        "Config file: ERROR: Config file is required\n",
        formatWithABlankConfigAnswer().stderr(),
        "expected the prompt and a one-line error, not a stack trace");
  }

  private Run executeAMissingSourceFile() throws Exception {
    return runCapturingStderr(
        "",
        "execute",
        SOURCE_FLAG,
        tempDir.resolve("missing.pisp").toString(),
        VERSION_FLAG,
        VERSION_ONE_ZERO);
  }

  @Test
  void aMissingSourceFileIsReportedOnOneLine() throws Exception {
    assertEquals(
        "ERROR: File not found: " + tempDir.resolve("missing.pisp") + "\n",
        executeAMissingSourceFile().stderr(),
        "expected a one-line error naming the missing file");
  }

  @Test
  void aMissingSourceFileExitsWithOne() throws Exception {
    assertEquals(1, executeAMissingSourceFile().exitCode(), EXPECTED_FAILURE_EXIT_CODE);
  }
}
