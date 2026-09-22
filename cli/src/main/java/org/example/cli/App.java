package org.example.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.printscript.application.AnalysisResult;
import org.printscript.application.CommandResult;
import org.printscript.application.JsonPrintScriptConfigReader;
import org.printscript.application.LanguageVersion;
import org.printscript.application.PrintScript;
import org.printscript.application.PrintScriptConfigReader;
import org.printscript.application.ProgressReporter;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.interpreter.EnvironmentPort;
import org.printscript.interpreter.InputPort;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * The CLI adapter: argument parsing, file/stdin/stdout wiring, and terminal rendering of
 * diagnostics and progress. Every subcommand ({@link ExecuteCommand}, {@link FormatCommand}, {@link
 * AnalyzeCommand}, {@link ValidateCommand}) does the I/O plumbing and then delegates the actual
 * work to a single shared {@link PrintScript} instance — no language logic lives here. {@code
 * readInput}/{@code readEnv} reach real stdin/the process environment only through {@link
 * InputPort}/{@link EnvironmentPort}, constructed here and nowhere else in this class's call chain.
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
    this(new JsonPrintScriptConfigReader());
  }

  App(PrintScriptConfigReader configReader) {
    this(configReader, App::promptConfigPath);
  }

  App(PrintScriptConfigReader configReader, ConfigPathPrompt configPathPrompt) {
    this(
        configReader,
        new PrintScript(),
        message -> System.err.println("[printscript] " + message),
        configPathPrompt);
  }

  App(PrintScriptConfigReader configReader, PrintScript printScript, ProgressReporter progress) {
    this(configReader, printScript, progress, App::promptConfigPath);
  }

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

  public static void main(String[] args) {
    int exitCode = new CommandLine(new App()).execute(args);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  int run(String... args) {
    return new CommandLine(this).execute(args);
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
        required = true)
    private String version;

    @Override
    @SuppressWarnings(
        "PMD.CloseResource") // stdin wraps System.in; closing it would close System.in itself
    public Integer call() throws IOException {
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
      InputPort input =
          prompt -> {
            System.out.print(prompt);
            System.out.flush();
            try {
              return stdin.readLine();
            } catch (IOException exception) {
              throw new UncheckedIOException(exception);
            }
          };
      EnvironmentPort env = name -> Optional.ofNullable(System.getenv(name));
      CommandResult<?> result =
          app.printScript.execute(
              Files.newBufferedReader(sourceFile),
              LanguageVersion.parse(version),
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
        required = true)
    private String version;

    @Option(names = {"-c", "--config"})
    private Path configFile;

    @Override
    public Integer call() throws IOException {
      Path resolvedConfigFile = app.resolveConfigFile(configFile);
      CommandResult<Void> result =
          app.printScript.format(
              Files.newBufferedReader(sourceFile),
              LanguageVersion.parse(version),
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
        required = true)
    private String version;

    @Option(names = {"-c", "--config"})
    private Path configFile;

    @Override
    public Integer call() throws IOException {
      Path resolvedConfigFile = app.resolveConfigFile(configFile);
      CommandResult<AnalysisResult> result =
          app.printScript.analyze(
              Files.newBufferedReader(sourceFile),
              LanguageVersion.parse(version),
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
        required = true)
    private String version;

    @Override
    public Integer call() throws IOException {
      CommandResult<Void> result =
          app.printScript.validate(
              Files.newBufferedReader(sourceFile), LanguageVersion.parse(version), app.progress);
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

  @FunctionalInterface
  interface ConfigPathPrompt {
    Path ask(String prompt) throws IOException;
  }
}
