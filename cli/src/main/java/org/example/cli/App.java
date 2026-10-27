package org.example.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.toolchain.AnalysisResult;
import org.printscript.toolchain.CommandResult;
import org.printscript.toolchain.EnvironmentSource;
import org.printscript.toolchain.InputSource;
import org.printscript.toolchain.JsonPrintScriptConfigReader;
import org.printscript.toolchain.LanguageVersion;
import org.printscript.toolchain.PrintScript;
import org.printscript.toolchain.PrintScriptConfigReader;
import org.printscript.toolchain.ProgressReporter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * The CLI adapter: argument parsing, file/stdin/stdout wiring, and terminal rendering of
 * diagnostics and progress. Every subcommand ({@link ExecuteCommand}, {@link FormatCommand}, {@link
 * AnalyzeCommand}, {@link ValidateCommand}) does the I/O plumbing and then delegates the actual
 * work to a single shared {@link PrintScript} instance — no language logic lives here. {@code
 * readInput}/{@code readEnv} reach real stdin/the process environment only through {@link
 * InputSource}/{@link EnvironmentSource}, constructed here and nowhere else in this class's call
 * chain.
 */
@Command(
    name = "printscript",
    mixinStandardHelpOptions = true,
    subcommands = {
      App.ExecuteCommand.class,
      App.FormatCommand.class,
      App.AnalyzeCommand.class,
      App.ValidateCommand.class
    })
public class App implements Callable<Integer> {
  private static final String SOURCE_OPTION = "--source";
  private static final String VERSION_OPTION = "--version";

  private final PrintScriptConfigReader configReader;
  private final PrintScript printScript;
  private final ProgressReporter progress;
  private final ConfigPathPrompt configPathPrompt;

  public App() {
    this(
        new JsonPrintScriptConfigReader(),
        new PrintScript(),
        message -> System.err.println("[printscript] " + message),
        App::promptConfigPath);
  }

  /** Injects every collaborator that touches the outside world: config files, stderr, stdin. */
  App(
      PrintScriptConfigReader configReader,
      PrintScript printScript,
      ProgressReporter progress,
      ConfigPathPrompt configPathPrompt) {
    this.configReader = configReader;
    this.printScript = printScript;
    this.progress = progress;
    this.configPathPrompt = configPathPrompt;
  }

  /**
   * The CLI for {@code app}, as {@link #main} runs it. A command that fails on I/O — a missing
   * source file, an unreadable or unanswered config path — is reported as one {@code ERROR:} line
   * on stderr with exit code 1, not a stack trace. Any other exception is a bug and keeps picocli's
   * default handling, stack trace included.
   */
  static CommandLine commandLine(App app) {
    return new CommandLine(app)
        .setExecutionExceptionHandler(
            (exception, commandLine, parseResult) -> reportIoFailure(exception));
  }

  public static void main(String[] args) {
    int exitCode = commandLine(new App()).execute(args);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  private static int reportIoFailure(Exception exception) throws Exception {
    IOException failure = asIoException(exception);
    if (failure == null) {
      throw exception;
    }
    System.err.println("ERROR: " + describe(failure));
    return 1;
  }

  private static IOException asIoException(Exception exception) {
    if (exception instanceof IOException io) {
      return io;
    }
    if (exception instanceof UncheckedIOException unchecked) {
      return unchecked.getCause();
    }
    return null;
  }

  private static String describe(IOException failure) {
    if (failure instanceof NoSuchFileException missing) {
      return "File not found: " + missing.getFile();
    }
    return failure.getMessage();
  }

  @Override
  public Integer call() {
    new CommandLine(this).usage(System.err);
    return 2;
  }

  @Command(name = "execute", mixinStandardHelpOptions = true)
  static final class ExecuteCommand implements Callable<Integer> {
    @CommandLine.ParentCommand private App app;

    @Option(
        names = {"-s", SOURCE_OPTION},
        required = true)
    private Path sourceFile;

    @Option(
        names = {"-v", VERSION_OPTION},
        required = true,
        converter = LanguageVersionConverter.class)
    private LanguageVersion version;

    @Override
    @SuppressWarnings(
        "PMD.CloseResource") // stdin wraps System.in; closing it would close System.in itself
    public Integer call() throws IOException {
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
      InputSource input =
          prompt -> {
            System.out.print(prompt);
            System.out.flush();
            try {
              return stdin.readLine();
            } catch (IOException exception) {
              throw new UncheckedIOException(exception);
            }
          };
      EnvironmentSource env = name -> Optional.ofNullable(System.getenv(name));
      CommandResult<?> result =
          app.printScript.execute(
              Files.newBufferedReader(sourceFile),
              version,
              System.out::println,
              input,
              env,
              app.progress);
      if (!result.isSuccess()) return app.printDiagnostics(result.diagnostics());
      return 0;
    }
  }

  @Command(name = "format", mixinStandardHelpOptions = true)
  static final class FormatCommand implements Callable<Integer> {
    @CommandLine.ParentCommand private App app;

    @Option(
        names = {"-s", SOURCE_OPTION},
        required = true)
    private Path sourceFile;

    @Option(
        names = {"-v", VERSION_OPTION},
        required = true,
        converter = LanguageVersionConverter.class)
    private LanguageVersion version;

    @Option(names = {"-c", "--config"})
    private Path configFile;

    @Override
    public Integer call() throws IOException {
      Path resolvedConfigFile = app.resolveConfigFile(configFile);
      CommandResult<Void> result =
          app.printScript.format(
              Files.newBufferedReader(sourceFile),
              version,
              app.configReader.readFormatterConfig(resolvedConfigFile),
              System.out,
              app.progress);
      if (!result.isSuccess()) return app.printDiagnostics(result.diagnostics());
      return 0;
    }
  }

  @Command(name = "analyze", mixinStandardHelpOptions = true)
  static final class AnalyzeCommand implements Callable<Integer> {
    @CommandLine.ParentCommand private App app;

    @Option(
        names = {"-s", SOURCE_OPTION},
        required = true)
    private Path sourceFile;

    @Option(
        names = {"-v", VERSION_OPTION},
        required = true,
        converter = LanguageVersionConverter.class)
    private LanguageVersion version;

    @Option(names = {"-c", "--config"})
    private Path configFile;

    @Override
    public Integer call() throws IOException {
      Path resolvedConfigFile = app.resolveConfigFile(configFile);
      CommandResult<AnalysisResult> result =
          app.printScript.analyze(
              Files.newBufferedReader(sourceFile),
              version,
              app.configReader.readAnalyzerConfig(resolvedConfigFile),
              app::printDiagnostic,
              app.progress);
      if (!result.isSuccess()) return app.printDiagnostics(result.diagnostics());
      return result.value().errorCount() == 0 ? 0 : 1;
    }
  }

  @Command(name = "validate", mixinStandardHelpOptions = true)
  static final class ValidateCommand implements Callable<Integer> {
    @CommandLine.ParentCommand private App app;

    @Option(
        names = {"-s", SOURCE_OPTION},
        required = true)
    private Path sourceFile;

    @Option(
        names = {"-v", VERSION_OPTION},
        required = true,
        converter = LanguageVersionConverter.class)
    private LanguageVersion version;

    @Override
    public Integer call() throws IOException {
      CommandResult<Void> result =
          app.printScript.validate(Files.newBufferedReader(sourceFile), version, app.progress);
      if (!result.isSuccess()) return app.printDiagnostics(result.diagnostics());
      return 0;
    }
  }

  private Path resolveConfigFile(Path configFile) throws IOException {
    if (configFile != null) return configFile;
    return configPathPrompt.ask("Config file: ");
  }

  private static Path promptConfigPath(String prompt) throws IOException {
    System.err.print(prompt);
    String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
    if (line == null || line.isBlank()) {
      throw new IOException("Config file is required");
    }
    return Path.of(line.trim());
  }

  private int printDiagnostics(List<Diagnostic> diagnostics) {
    for (Diagnostic diagnostic : diagnostics) {
      printDiagnostic(diagnostic);
    }
    return 1;
  }

  private void printDiagnostic(Diagnostic diagnostic) {
    var start = diagnostic.span().start();
    var end = diagnostic.span().end();
    System.err.printf(
        "%s %s at row %d column %d to row %d column %d: %s%n",
        diagnostic.severity(),
        diagnostic.phase(),
        start.row(),
        start.column(),
        end.row(),
        end.column(),
        diagnostic.message());
  }

  /**
   * Rejects an unsupported {@code --version} while picocli parses arguments, so it's reported as a
   * usage error (exit code 2) like any other invalid flag, before any command runs.
   */
  static final class LanguageVersionConverter
      implements CommandLine.ITypeConverter<LanguageVersion> {
    @Override
    public LanguageVersion convert(String value) {
      try {
        return LanguageVersion.parse(value);
      } catch (IllegalArgumentException exception) {
        var conversionError = new CommandLine.TypeConversionException(exception.getMessage());
        conversionError.initCause(exception);
        throw conversionError;
      }
    }
  }

  @FunctionalInterface
  interface ConfigPathPrompt {
    Path ask(String prompt) throws IOException;
  }
}
