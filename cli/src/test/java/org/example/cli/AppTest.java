package org.example.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.application.PrintScript;
import org.printscript.application.PrintScriptConfigReader;
import org.printscript.application.ProgressReporter;
import org.printscript.formatter.FormatterConfig;

class AppTest {
  private static final String SOURCE_FILE_NAME = "source.pisp";
  private static final String SOURCE_FLAG = "--source";
  private static final String VERSION_FLAG = "--version";
  private static final String CONFIG_FLAG = "--config";
  private static final String CONFIG_FILE_NAME = "config.toml";
  private static final String EXPECTED_SUCCESSFUL_EXIT_CODE = "expected successful exit code";
  private static final String CLOSE_RESOURCE_SUPPRESSION = "PMD.CloseResource";
  private static final String FORMATTER_SECTION = "[formatter]\n";
  private static final String ANALYZER_SECTION = "[analyzer]\n";
  private static final String FORMAT_COMMAND = "format";
  private static final String ANALYZE_COMMAND = "analyze";
  private static final String EXECUTE_COMMAND = "execute";
  private static final String VALIDATE_COMMAND = "validate";
  private static final String VERSION_ONE_ZERO = "1.0";
  private static final String UNSUPPORTED_VERSION = "9.9";
  private static final String UNSUPPORTED_VERSION_FRAGMENT = "Unsupported PrintScript version";
  private static final String EXPECTED_UNSUPPORTED_VERSION_DIAGNOSTIC =
      "expected the unsupported-version diagnostic to be printed to stderr";
  private static final String PRINTLN_HI_SOURCE = "println(\"hi\");";

  @TempDir Path tempDir;

  @Test
  void executeCommandReadingStdinExitsSuccessfully() throws Exception {
    ExecuteRun run = runExecuteCommandWithStdin();

    assertEquals(0, run.exitCode(), EXPECTED_SUCCESSFUL_EXIT_CODE);
  }

  @Test
  void executeCommandReadingStdinWritesPromptAndOutput() throws Exception {
    ExecuteRun run = runExecuteCommandWithStdin();

    assertEquals(
        "Name: Ada\n", run.stdout(), "expected the prompt then the println output on stdout");
  }

  @SuppressWarnings(
      CLOSE_RESOURCE_SUPPRESSION) // originalOut/originalIn are saved references to restore, not
  // ours to close
  private ExecuteRun runExecuteCommandWithStdin() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Files.writeString(source, "let name: string = readInput(\"Name: \");\nprintln(name);");

    int exitCode;
    String stdout;
    PrintStream originalOut = System.out;
    InputStream originalIn = System.in;
    try (ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        PrintStream redirectedOut = new PrintStream(capturedOut, true, StandardCharsets.UTF_8)) {
      System.setOut(redirectedOut);
      System.setIn(new ByteArrayInputStream("Ada\n".getBytes(StandardCharsets.UTF_8)));
      exitCode =
          new App().run(EXECUTE_COMMAND, SOURCE_FLAG, source.toString(), VERSION_FLAG, "1.1");
      stdout = capturedOut.toString(StandardCharsets.UTF_8);
    } finally {
      System.setOut(originalOut);
      System.setIn(originalIn);
    }
    return new ExecuteRun(exitCode, stdout);
  }

  private record ExecuteRun(int exitCode, String stdout) {}

  @Test
  void formatCommandExitsSuccessfully() throws Exception {
    FormatRun run = runFormatCommandWithInjectedConfigReader();

    assertEquals(0, run.exitCode(), EXPECTED_SUCCESSFUL_EXIT_CODE);
  }

  @Test
  void formatCommandUsesInjectedConfigReader() throws Exception {
    FormatRun run = runFormatCommandWithInjectedConfigReader();

    assertEquals(
        run.config(), run.reader().formatterPath, "expected injected config path to be used");
  }

  @Test
  void formatCommandWritesFormattedSourceToStdout() throws Exception {
    FormatRun run = runFormatCommandWithInjectedConfigReader();

    assertEquals(
        "let text: string = \"hello\";\nprintln(text);\n",
        run.stdout(),
        "expected formatted source on stdout");
  }

  @SuppressWarnings(
      CLOSE_RESOURCE_SUPPRESSION) // originalOut is a saved reference to restore, not ours to close
  private FormatRun runFormatCommandWithInjectedConfigReader() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Path config = tempDir.resolve(CONFIG_FILE_NAME);
    Files.writeString(source, "let text: string = \"hello\";\nprintln(text);");
    Files.writeString(config, FORMATTER_SECTION);
    FakeConfigReader reader = new FakeConfigReader();

    int exitCode;
    String stdout;
    PrintStream originalOut = System.out;
    try (ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        PrintStream redirectedOut = new PrintStream(capturedOut, true, StandardCharsets.UTF_8)) {
      System.setOut(redirectedOut);
      exitCode =
          new App(reader)
              .run(
                  FORMAT_COMMAND,
                  SOURCE_FLAG,
                  source.toString(),
                  VERSION_FLAG,
                  VERSION_ONE_ZERO,
                  CONFIG_FLAG,
                  config.toString());
      stdout = capturedOut.toString(StandardCharsets.UTF_8);
    } finally {
      System.setOut(originalOut);
    }
    return new FormatRun(exitCode, config, reader, stdout);
  }

  @Test
  void formatCommandPromptsForMissingConfigFileExitsSuccessfully() throws Exception {
    PromptFormatRun run = runFormatCommandWithMissingConfigFile();

    assertEquals(0, run.exitCode(), EXPECTED_SUCCESSFUL_EXIT_CODE);
  }

  @Test
  void formatCommandPromptsForMissingConfigFileUsesPromptedPath() throws Exception {
    PromptFormatRun run = runFormatCommandWithMissingConfigFile();

    assertEquals(
        run.config(), run.reader().formatterPath, "expected prompted config path to be used");
  }

  @Test
  void formatCommandPromptsForMissingConfigFileAsksExpectedPrompt() throws Exception {
    PromptFormatRun run = runFormatCommandWithMissingConfigFile();

    assertEquals(List.of("Config file: "), run.prompts(), "expected a single config-file prompt");
  }

  @Test
  void formatCommandPromptsForMissingConfigFileWritesFormattedSourceToStdout() throws Exception {
    PromptFormatRun run = runFormatCommandWithMissingConfigFile();

    assertEquals(
        "let text: string = \"hello\";\n", run.stdout(), "expected formatted source on stdout");
  }

  @SuppressWarnings(
      CLOSE_RESOURCE_SUPPRESSION) // originalOut is a saved reference to restore, not ours to close
  private PromptFormatRun runFormatCommandWithMissingConfigFile() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Path config = tempDir.resolve(CONFIG_FILE_NAME);
    Files.writeString(source, "let text: string = \"hello\";");
    Files.writeString(config, FORMATTER_SECTION);
    FakeConfigReader reader = new FakeConfigReader();
    PromptStub prompt = new PromptStub(config);

    int exitCode;
    String stdout;
    PrintStream originalOut = System.out;
    try (ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        PrintStream redirectedOut = new PrintStream(capturedOut, true, StandardCharsets.UTF_8)) {
      System.setOut(redirectedOut);
      exitCode =
          new App(reader, prompt)
              .run(FORMAT_COMMAND, SOURCE_FLAG, source.toString(), VERSION_FLAG, VERSION_ONE_ZERO);
      stdout = capturedOut.toString(StandardCharsets.UTF_8);
    } finally {
      System.setOut(originalOut);
    }
    return new PromptFormatRun(exitCode, config, reader, prompt.prompts, stdout);
  }

  @Test
  void analyzeCommandPromptsForMissingConfigFileExitsSuccessfully() throws Exception {
    AnalyzeRun run = runAnalyzeCommandWithMissingConfigFile();

    assertEquals(0, run.exitCode(), EXPECTED_SUCCESSFUL_EXIT_CODE);
  }

  @Test
  void analyzeCommandPromptsForMissingConfigFileUsesPromptedPath() throws Exception {
    AnalyzeRun run = runAnalyzeCommandWithMissingConfigFile();

    assertEquals(
        run.config(), run.reader().analyzerPath, "expected prompted config path to be used");
  }

  @Test
  void analyzeCommandPromptsForMissingConfigFileAsksExpectedPrompt() throws Exception {
    AnalyzeRun run = runAnalyzeCommandWithMissingConfigFile();

    assertEquals(List.of("Config file: "), run.prompts(), "expected a single config-file prompt");
  }

  private AnalyzeRun runAnalyzeCommandWithMissingConfigFile() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Path config = tempDir.resolve(CONFIG_FILE_NAME);
    Files.writeString(source, "let text: string = \"hello\";");
    Files.writeString(config, ANALYZER_SECTION);
    FakeConfigReader reader = new FakeConfigReader();
    PromptStub prompt = new PromptStub(config);

    int exitCode =
        new App(reader, prompt)
            .run(ANALYZE_COMMAND, SOURCE_FLAG, source.toString(), VERSION_FLAG, VERSION_ONE_ZERO);

    return new AnalyzeRun(exitCode, config, reader, prompt.prompts);
  }

  @Test
  void validateCommandExitsSuccessfullyForValidSource() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Files.writeString(source, "let name: string = \"Ada\";");

    int exitCode =
        new App()
            .run(VALIDATE_COMMAND, SOURCE_FLAG, source.toString(), VERSION_FLAG, VERSION_ONE_ZERO);

    assertEquals(0, exitCode, EXPECTED_SUCCESSFUL_EXIT_CODE);
  }

  @Test
  void validateCommandWithUnsupportedVersionFailsWithExitCodeOne() throws Exception {
    CapturedErr captured = runValidateWithUnsupportedVersion();

    assertEquals(1, captured.exitCode(), "expected validate to fail for an unsupported version");
  }

  @Test
  void validateCommandWithUnsupportedVersionPrintsDiagnostic() throws Exception {
    CapturedErr captured = runValidateWithUnsupportedVersion();

    assertTrue(
        captured.stderr().contains(UNSUPPORTED_VERSION_FRAGMENT),
        EXPECTED_UNSUPPORTED_VERSION_DIAGNOSTIC);
  }

  private CapturedErr runValidateWithUnsupportedVersion() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Files.writeString(source, "let name: string = \"Ada\";");

    return runCapturingStderr(
        () ->
            new App()
                .run(
                    VALIDATE_COMMAND,
                    SOURCE_FLAG,
                    source.toString(),
                    VERSION_FLAG,
                    UNSUPPORTED_VERSION));
  }

  @Test
  void executeCommandWithUnsupportedVersionFailsWithExitCodeOne() throws Exception {
    CapturedErr captured = runExecuteWithUnsupportedVersion();

    assertEquals(1, captured.exitCode(), "expected execute to fail for an unsupported version");
  }

  @Test
  void executeCommandWithUnsupportedVersionPrintsDiagnostic() throws Exception {
    CapturedErr captured = runExecuteWithUnsupportedVersion();

    assertTrue(
        captured.stderr().contains(UNSUPPORTED_VERSION_FRAGMENT),
        EXPECTED_UNSUPPORTED_VERSION_DIAGNOSTIC);
  }

  private CapturedErr runExecuteWithUnsupportedVersion() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Files.writeString(source, PRINTLN_HI_SOURCE);

    return runCapturingStderr(
        () ->
            new App()
                .run(
                    EXECUTE_COMMAND,
                    SOURCE_FLAG,
                    source.toString(),
                    VERSION_FLAG,
                    UNSUPPORTED_VERSION));
  }

  @Test
  void formatCommandWithUnsupportedVersionFailsWithExitCodeOne() throws Exception {
    CapturedErr captured = runFormatWithUnsupportedVersion();

    assertEquals(1, captured.exitCode(), "expected format to fail for an unsupported version");
  }

  @Test
  void formatCommandWithUnsupportedVersionPrintsDiagnostic() throws Exception {
    CapturedErr captured = runFormatWithUnsupportedVersion();

    assertTrue(
        captured.stderr().contains(UNSUPPORTED_VERSION_FRAGMENT),
        EXPECTED_UNSUPPORTED_VERSION_DIAGNOSTIC);
  }

  private CapturedErr runFormatWithUnsupportedVersion() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Path config = tempDir.resolve(CONFIG_FILE_NAME);
    Files.writeString(source, PRINTLN_HI_SOURCE);
    Files.writeString(config, FORMATTER_SECTION);

    return runCapturingStderr(
        () ->
            new App(new FakeConfigReader())
                .run(
                    FORMAT_COMMAND,
                    SOURCE_FLAG,
                    source.toString(),
                    VERSION_FLAG,
                    UNSUPPORTED_VERSION,
                    CONFIG_FLAG,
                    config.toString()));
  }

  @Test
  void analyzeCommandWithUnsupportedVersionFailsWithExitCodeOne() throws Exception {
    CapturedErr captured = runAnalyzeWithUnsupportedVersion();

    assertEquals(1, captured.exitCode(), "expected analyze to fail for an unsupported version");
  }

  @Test
  void analyzeCommandWithUnsupportedVersionPrintsDiagnostic() throws Exception {
    CapturedErr captured = runAnalyzeWithUnsupportedVersion();

    assertTrue(
        captured.stderr().contains(UNSUPPORTED_VERSION_FRAGMENT),
        EXPECTED_UNSUPPORTED_VERSION_DIAGNOSTIC);
  }

  private CapturedErr runAnalyzeWithUnsupportedVersion() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Path config = tempDir.resolve(CONFIG_FILE_NAME);
    Files.writeString(source, PRINTLN_HI_SOURCE);
    Files.writeString(config, ANALYZER_SECTION);

    return runCapturingStderr(
        () ->
            new App(new FakeConfigReader())
                .run(
                    ANALYZE_COMMAND,
                    SOURCE_FLAG,
                    source.toString(),
                    VERSION_FLAG,
                    UNSUPPORTED_VERSION,
                    CONFIG_FLAG,
                    config.toString()));
  }

  @Test
  void analyzeCommandReturnsExitCodeOneWhenErrorDiagnosticsAreFound() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Path config = tempDir.resolve(CONFIG_FILE_NAME);
    Files.writeString(source, "let badName: string = \"hello\";");
    Files.writeString(config, ANALYZER_SECTION);

    int exitCode =
        new App(new FakeConfigReader())
            .run(
                ANALYZE_COMMAND,
                SOURCE_FLAG,
                source.toString(),
                VERSION_FLAG,
                VERSION_ONE_ZERO,
                CONFIG_FLAG,
                config.toString());

    assertEquals(1, exitCode, "expected a naming-style violation to yield exit code 1");
  }

  @Test
  void runningWithoutASubcommandPrintsUsageAndReturnsExitCodeTwo() {
    int exitCode = new App().run();

    assertEquals(2, exitCode, "expected the bare command to print usage and exit with code 2");
  }

  @Test
  void mainWithHelpFlagExitsWithoutThrowing() {
    assertDoesNotThrow(
        () -> App.main(new String[] {"--help"}),
        "expected --help to exit cleanly without System.exit");
  }

  @Test
  void appConstructedWithExplicitPrintScriptAndProgressExitsSuccessfully() throws Exception {
    InjectedProgressRun run = runWithInjectedPrintScriptAndProgress();

    assertEquals(0, run.exitCode(), EXPECTED_SUCCESSFUL_EXIT_CODE);
  }

  @Test
  void appConstructedWithExplicitPrintScriptAndProgressUsesTheInjectedProgressReporter()
      throws Exception {
    InjectedProgressRun run = runWithInjectedPrintScriptAndProgress();

    assertNotEquals(
        List.of(), run.progressMessages(), "expected the injected progress reporter to be used");
  }

  private InjectedProgressRun runWithInjectedPrintScriptAndProgress() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Files.writeString(source, PRINTLN_HI_SOURCE);
    List<String> progressMessages = new ArrayList<>();
    ProgressReporter progress = progressMessages::add;

    int exitCode =
        new App(new FakeConfigReader(), new PrintScript(), progress)
            .run(EXECUTE_COMMAND, SOURCE_FLAG, source.toString(), VERSION_FLAG, VERSION_ONE_ZERO);

    return new InjectedProgressRun(exitCode, progressMessages);
  }

  private record InjectedProgressRun(int exitCode, List<String> progressMessages) {}

  @Test
  @SuppressWarnings(
      CLOSE_RESOURCE_SUPPRESSION) // the ByteArrayInputStream backs System.in for the duration of
  // the test and is restored, not ours to close
  void formatCommandWithDefaultAppPromptsRealStdinForConfigPath() throws Exception {
    Path source = tempDir.resolve(SOURCE_FILE_NAME);
    Path config = tempDir.resolve(CONFIG_FILE_NAME);
    Files.writeString(source, "let text: string = \"hello\";");
    Files.writeString(config, FORMATTER_SECTION);

    int exitCode;
    InputStream originalIn = System.in;
    try {
      System.setIn(
          new ByteArrayInputStream((config.toString() + "\n").getBytes(StandardCharsets.UTF_8)));
      exitCode =
          new App()
              .run(FORMAT_COMMAND, SOURCE_FLAG, source.toString(), VERSION_FLAG, VERSION_ONE_ZERO);
    } finally {
      System.setIn(originalIn);
    }

    assertEquals(0, exitCode, "expected the real stdin prompt to supply the config path");
  }

  private interface ThrowingSupplier {
    int run() throws Exception;
  }

  @SuppressWarnings(
      CLOSE_RESOURCE_SUPPRESSION) // originalErr is a saved reference to restore, not ours to close
  private CapturedErr runCapturingStderr(ThrowingSupplier action) throws Exception {
    PrintStream originalErr = System.err;
    int exitCode;
    String stderr;
    try (ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream redirectedErr = new PrintStream(capturedErr, true, StandardCharsets.UTF_8)) {
      System.setErr(redirectedErr);
      exitCode = action.run();
      stderr = capturedErr.toString(StandardCharsets.UTF_8);
    } finally {
      System.setErr(originalErr);
    }
    return new CapturedErr(exitCode, stderr);
  }

  private record CapturedErr(int exitCode, String stderr) {}

  private record FormatRun(int exitCode, Path config, FakeConfigReader reader, String stdout) {}

  private record PromptFormatRun(
      int exitCode, Path config, FakeConfigReader reader, List<String> prompts, String stdout) {}

  private record AnalyzeRun(
      int exitCode, Path config, FakeConfigReader reader, List<String> prompts) {}

  private static final class FakeConfigReader implements PrintScriptConfigReader {
    private Path formatterPath;
    private Path analyzerPath;

    @Override
    public FormatterConfig readFormatterConfig(Path path) {
      formatterPath = path;
      return new FormatterConfig(0, 0, 1, 1, 1, 2);
    }

    @Override
    public AnalyzerConfig readAnalyzerConfig(Path path) {
      analyzerPath = path;
      return AnalyzerConfig.defaults();
    }
  }

  private static final class PromptStub implements App.ConfigPathPrompt {
    private final Path path;
    private final List<String> prompts = new ArrayList<>();

    private PromptStub(Path path) {
      this.path = path;
    }

    @Override
    public Path ask(String prompt) {
      prompts.add(prompt);
      return path;
    }
  }
}
