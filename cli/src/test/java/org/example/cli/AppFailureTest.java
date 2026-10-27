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
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.formatter.FormatterConfig;
import org.printscript.testkit.TestConfigs;
import org.printscript.toolchain.PrintScriptConfigReader;

/** Commands that fail on the user's code, and the real stdin prompt for a missing config file. */
class AppFailureTest {
  private static final String CLOSE_RESOURCE_SUPPRESSION = "PMD.CloseResource";
  private static final String EXPECTED_FAILURE_EXIT_CODE = "expected exit code 1";
  private static final String SOURCE_FLAG = "--source";
  private static final String VERSION_FLAG = "--version";
  private static final String CONFIG_FLAG = "--config";
  private static final String VERSION_ONE_ZERO = "1.0";

  @TempDir Path tempDir;

  private static final PrintScriptConfigReader DEFAULT_CONFIGS =
      new PrintScriptConfigReader() {
        @Override
        public FormatterConfig readFormatterConfig(Path path) {
          return TestConfigs.formatterDefaults();
        }

        @Override
        public AnalyzerConfig readAnalyzerConfig(Path path) {
          return TestConfigs.analyzerDefaults();
        }
      };

  /** Runs the CLI with {@code stdin} as standard input, discarding stdout and stderr. */
  @SuppressWarnings(CLOSE_RESOURCE_SUPPRESSION) // the saved streams are restored, not ours to close
  private int run(String stdin, String... args) throws Exception {
    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    try (PrintStream discard =
        new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
      System.setIn(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));
      System.setOut(discard);
      System.setErr(discard);
      return new App(DEFAULT_CONFIGS).run(args);
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
}
