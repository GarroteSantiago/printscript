package org.printscript.application;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.analyzer.NamingStyleRules;
import org.printscript.analyzer.StaticAnalyzer;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Phase;
import org.printscript.diagnostics.Severity;
import org.printscript.formatter.FormatterConfigProvider;
import org.printscript.formatter.PrintScriptFormatter;
import org.printscript.formatter.SpacingRules;
import org.printscript.interpreter.ArithmeticOperators;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.RuntimeEnvironment;
import org.printscript.interpreter.RuntimeFailure;
import org.printscript.lexer.KeywordTable;
import org.printscript.lexer.Lexer;
import org.printscript.parser.StatementSyntaxReader;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;
import org.printscript.tokens.SyntaxException;
import org.printscript.typechecker.SemanticContext;
import org.printscript.typechecker.ValidatingStatementSource;
import org.printscript.typetable.SemanticException;

/**
 * The use-case facade and composition root for PrintScript: {@link #execute}, {@link #format},
 * {@link #analyze}, and {@link #validate} are the only operations any interaction layer (currently
 * the {@code cli} module) needs. Every method returns a {@link CommandResult} and never lets a
 * {@link SyntaxException}, {@link SemanticException}, or {@link RuntimeFailure} escape — all three
 * are caught here and converted to a failed {@code CommandResult} carrying the underlying {@link
 * Diagnostic}(s).
 *
 * <p>This is the one place (besides {@code testkit}, for tests) allowed to construct a concrete
 * {@code lexer.Lexer}, wire it into a {@code parser.StatementSyntaxReader}, and wrap that into a
 * {@link ValidatingStatementSource} — every stage downstream of that depends only on the {@code
 * TokenSource}/{@code StatementSource}/{@code typetable.ValidatedStatementSource} ports, and
 * consumes/folds the resulting stream itself ({@code Interpreter#executeAll}, {@code
 * StaticAnalyzer#analyzeAll}, {@code PrintScriptFormatter#formatAll}) rather than being driven
 * statement-by-statement from here. {@link #pipelineFor} is where every per-version strategy this
 * class itself constructs ({@link KeywordTable}, {@link ArithmeticOperators}, {@link
 * NamingStyleRules}, {@code SpacingRules}) gets selected for a requested {@link LanguageVersion};
 * {@code typetable.BuiltinRegistry}/{@code types.TypeAnnotationTable} selection lives behind {@link
 * SemanticContext#forVersion} instead, since this class never otherwise touches that vocabulary.
 */
public final class PrintScript {
  private static final String READING_STATEMENTS = "Reading statements";

  private record LanguagePipeline(
      KeywordTable keywords,
      boolean v11,
      ArithmeticOperators operators,
      StaticAnalyzer staticAnalyzer,
      PrintScriptFormatter formatter) {}

  private LanguagePipeline pipelineFor(LanguageVersion version) {
    boolean v11 = version.supportsV1_1();
    return new LanguagePipeline(
        v11 ? KeywordTable.v1_1() : KeywordTable.v1(),
        v11,
        ArithmeticOperators.v1(),
        new StaticAnalyzer(NamingStyleRules.v1()),
        new PrintScriptFormatter(v11 ? SpacingRules.v1_1() : SpacingRules.v1()));
  }

  private boolean supported(LanguageVersion version) {
    return version.supportsV1() || version.supportsV1_1();
  }

  public CommandResult<ExecutionResult> execute(
      String source, LanguageVersion version, ProgressReporter progress) {
    return execute(new StringReader(source), version, progress);
  }

  public CommandResult<ExecutionResult> execute(
      Reader source, LanguageVersion version, ProgressReporter progress) {
    List<String> output = new ArrayList<>();
    CommandResult<RuntimeEnvironment> result = execute(source, version, output::add, progress);
    if (!result.isSuccess()) return CommandResult.failure(result.diagnostics());
    return CommandResult.success(new ExecutionResult(output));
  }

  public CommandResult<RuntimeEnvironment> execute(
      Reader source, LanguageVersion version, Consumer<String> output, ProgressReporter progress) {
    if (!supported(version)) return unsupported(version);
    LanguagePipeline pipeline = pipelineFor(version);
    Interpreter interpreter = new Interpreter(output::accept, pipeline.operators());
    return execute(source, pipeline, interpreter, progress);
  }

  public CommandResult<RuntimeEnvironment> execute(
      Reader source,
      LanguageVersion version,
      Consumer<String> output,
      InputSource input,
      EnvironmentSource env,
      ProgressReporter progress) {
    if (!supported(version)) return unsupported(version);
    LanguagePipeline pipeline = pipelineFor(version);
    Interpreter interpreter =
        new Interpreter(output::accept, pipeline.operators(), input::readLine, env::get);
    return execute(source, pipeline, interpreter, progress);
  }

  private CommandResult<RuntimeEnvironment> execute(
      Reader source,
      LanguagePipeline pipeline,
      Interpreter interpreter,
      ProgressReporter progress) {
    try {
      progress.report(READING_STATEMENTS);
      var statements = new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
      var validated =
          new ValidatingStatementSource(statements, SemanticContext.forVersion(pipeline.v11()));
      progress.report("Executing statements");
      return CommandResult.success(interpreter.executeAll(validated, RuntimeEnvironment.empty()));
    } catch (RuntimeFailure failure) {
      return CommandResult.failure(List.of(failure.diagnostic()));
    } catch (SyntaxException exception) {
      return CommandResult.failure(List.of(exception.diagnostic()));
    } catch (SemanticException exception) {
      return CommandResult.failure(exception.diagnostics());
    }
  }

  public CommandResult<String> format(
      String source,
      LanguageVersion version,
      FormatterConfigProvider config,
      ProgressReporter progress) {
    return format(new StringReader(source), version, config, progress);
  }

  public CommandResult<String> format(
      Reader source,
      LanguageVersion version,
      FormatterConfigProvider config,
      ProgressReporter progress) {
    StringWriter output = new StringWriter();
    CommandResult<Void> result;
    try {
      result = format(source, version, config, output, progress);
    } catch (IOException exception) {
      throw new IllegalStateException("StringWriter append failed", exception);
    }
    if (!result.isSuccess()) return CommandResult.failure(result.diagnostics());
    return CommandResult.success(output.toString());
  }

  public CommandResult<Void> format(
      Reader source,
      LanguageVersion version,
      FormatterConfigProvider config,
      Appendable output,
      ProgressReporter progress)
      throws IOException {
    if (!supported(version)) return unsupported(version);
    LanguagePipeline pipeline = pipelineFor(version);
    try {
      progress.report(READING_STATEMENTS);
      var statements = new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
      progress.report("Formatting statements");
      pipeline.formatter().formatAll(statements, config, output);
      return CommandResult.success(null);
    } catch (SyntaxException exception) {
      return CommandResult.failure(List.of(exception.diagnostic()));
    }
  }

  public CommandResult<List<Diagnostic>> analyze(
      String source, LanguageVersion version, AnalyzerConfig config, ProgressReporter progress) {
    return analyze(new StringReader(source), version, config, progress);
  }

  public CommandResult<List<Diagnostic>> analyze(
      Reader source, LanguageVersion version, AnalyzerConfig config, ProgressReporter progress) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    CommandResult<AnalysisResult> result =
        analyze(source, version, config, diagnostics::add, progress);
    return new CommandResult<>(
        diagnostics, result.isSuccess() ? diagnostics : result.diagnostics());
  }

  public CommandResult<AnalysisResult> analyze(
      Reader source,
      LanguageVersion version,
      AnalyzerConfig config,
      Consumer<Diagnostic> diagnosticSink,
      ProgressReporter progress) {
    if (!supported(version)) return unsupported(version);
    LanguagePipeline pipeline = pipelineFor(version);
    AtomicInteger diagnosticCount = new AtomicInteger();
    AtomicInteger errorCount = new AtomicInteger();
    try {
      progress.report(READING_STATEMENTS);
      var statements = new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
      var validated =
          new ValidatingStatementSource(statements, SemanticContext.forVersion(pipeline.v11()));
      progress.report("Analyzing statements");
      pipeline
          .staticAnalyzer()
          .analyzeAll(
              validated,
              config,
              diagnostic -> {
                diagnosticSink.accept(diagnostic);
                diagnosticCount.incrementAndGet();
                if (diagnostic.severity() == Severity.ERROR) {
                  errorCount.incrementAndGet();
                }
              });
      return CommandResult.success(new AnalysisResult(diagnosticCount.get(), errorCount.get()));
    } catch (SyntaxException exception) {
      return CommandResult.failure(List.of(exception.diagnostic()));
    } catch (SemanticException exception) {
      return CommandResult.failure(exception.diagnostics());
    }
  }

  public CommandResult<Void> validate(
      String source, LanguageVersion version, ProgressReporter progress) {
    return validate(new StringReader(source), version, progress);
  }

  public CommandResult<Void> validate(
      Reader source, LanguageVersion version, ProgressReporter progress) {
    if (!supported(version)) return unsupported(version);
    LanguagePipeline pipeline = pipelineFor(version);
    try {
      progress.report(READING_STATEMENTS);
      var statements = new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
      var validated =
          new ValidatingStatementSource(statements, SemanticContext.forVersion(pipeline.v11()));
      validated.drain();
      return CommandResult.success(null);
    } catch (SyntaxException exception) {
      return CommandResult.failure(List.of(exception.diagnostic()));
    } catch (SemanticException exception) {
      return CommandResult.failure(exception.diagnostics());
    }
  }

  private <T> CommandResult<T> unsupported(LanguageVersion version) {
    Diagnostic diagnostic =
        Diagnostic.error(
            Phase.APPLICATION,
            "Unsupported PrintScript version: "
                + version.major()
                + "."
                + version.minor()
                + "."
                + version.patch(),
            new SourceSpan(new SourcePosition(1, 1, 0), new SourcePosition(1, 1, 0)));
    return CommandResult.failure(List.of(diagnostic));
  }
}
