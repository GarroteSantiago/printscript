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
import org.printscript.interpreter.EnvironmentPort;
import org.printscript.interpreter.InputPort;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.RuntimeEnvironment;
import org.printscript.interpreter.RuntimeFailure;
import org.printscript.lexer.KeywordTable;
import org.printscript.lexer.Lexer;
import org.printscript.parser.StatementSyntaxReader;
import org.printscript.semantics.BinaryOperatorRules;
import org.printscript.semantics.BuiltinRegistry;
import org.printscript.semantics.SemanticContext;
import org.printscript.semantics.SemanticStatementResult;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;
import org.printscript.syntax.StatementSource;
import org.printscript.syntax.TypeAnnotationTable;
import org.printscript.syntax.nodes.statements.StatementSyntax;
import org.printscript.tokens.SyntaxException;

/**
 * The use-case facade and composition root for PrintScript: {@link #execute}, {@link #format},
 * {@link #analyze}, and {@link #validate} are the only operations any interaction layer (currently
 * the {@code cli} module) needs. Every method returns a {@link CommandResult} and never lets a
 * {@link SyntaxException} or {@link RuntimeFailure} escape — both are caught here and converted to
 * a failed {@code CommandResult} carrying the underlying {@link Diagnostic}.
 *
 * <p>This is the one place (besides {@code testkit}, for tests) allowed to construct a concrete
 * {@code lexer.Lexer} and wire it into a {@code syntax.StatementSyntaxReader} — every stage in
 * between depends only on the {@code TokenSource}/{@code StatementSource} ports. {@link
 * #pipelineFor} is also where every per-version strategy ({@link KeywordTable}, {@link
 * TypeAnnotationTable}, {@link BinaryOperatorRules}, {@link ArithmeticOperators}, {@link
 * NamingStyleRules}, {@code SpacingRules}) gets selected for a requested {@link LanguageVersion} —
 * none of the classes receiving them decide their own version-specific behavior. All four commands
 * validate and process one statement at a time off a single {@code StatementSource}, rather than
 * building a whole {@code ProgramSyntax} first: semantic validation for a statement must succeed
 * before that statement is executed/formatted/analyzed, and a later statement is never even parsed
 * once an earlier one has failed.
 */
public final class PrintScript {
  private static final String READING_STATEMENTS = "Reading statements";

  private record LanguagePipeline(
      KeywordTable keywords,
      TypeAnnotationTable typeAnnotations,
      BinaryOperatorRules binaryOperatorRules,
      ArithmeticOperators operators,
      BuiltinRegistry builtins,
      StaticAnalyzer staticAnalyzer,
      PrintScriptFormatter formatter) {}

  private LanguagePipeline pipelineFor(LanguageVersion version) {
    boolean v11 = version.supportsV1_1();
    return new LanguagePipeline(
        v11 ? KeywordTable.v1_1() : KeywordTable.v1(),
        v11 ? TypeAnnotationTable.v1_1() : TypeAnnotationTable.v1(),
        BinaryOperatorRules.v1(),
        ArithmeticOperators.v1(),
        v11 ? BuiltinRegistry.v1_1() : BuiltinRegistry.v1(),
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
      InputPort input,
      EnvironmentPort env,
      ProgressReporter progress) {
    if (!supported(version)) return unsupported(version);
    LanguagePipeline pipeline = pipelineFor(version);
    Interpreter interpreter = new Interpreter(output::accept, pipeline.operators(), input, env);
    return execute(source, pipeline, interpreter, progress);
  }

  private CommandResult<RuntimeEnvironment> execute(
      Reader source,
      LanguagePipeline pipeline,
      Interpreter interpreter,
      ProgressReporter progress) {
    SemanticContext semanticContext =
        SemanticContext.empty(
            pipeline.builtins(), pipeline.typeAnnotations(), pipeline.binaryOperatorRules());
    RuntimeEnvironment runtimeEnvironment = RuntimeEnvironment.empty();
    try {
      progress.report(READING_STATEMENTS);
      StatementSource statements =
          new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
      while (statements.hasNext()) {
        StatementSyntax statement = statements.next();
        SemanticStatementResult semantic = semanticContext.validate(statement);
        if (!semantic.isSuccess()) return CommandResult.failure(semantic.diagnostics());
        progress.report("Executing statement");
        runtimeEnvironment =
            interpreter.executeStatement(statement, runtimeEnvironment, semantic.semanticModel());
        semanticContext = semantic.nextContext();
      }
    } catch (RuntimeFailure failure) {
      return CommandResult.failure(List.of(failure.diagnostic()));
    } catch (SyntaxException exception) {
      return CommandResult.failure(List.of(exception.diagnostic()));
    }
    return CommandResult.success(runtimeEnvironment);
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
      StatementSource statements =
          new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
      PrintScriptFormatter.Session session = pipeline.formatter().newSession(config);
      while (statements.hasNext()) {
        progress.report("Formatting statement");
        session.format(statements.next(), output);
      }
      session.finish(statements.eof(), output);
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
    SemanticContext semanticContext =
        SemanticContext.empty(
            pipeline.builtins(), pipeline.typeAnnotations(), pipeline.binaryOperatorRules());
    try {
      progress.report(READING_STATEMENTS);
      StatementSource statements =
          new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
      while (statements.hasNext()) {
        StatementSyntax statement = statements.next();
        SemanticStatementResult semantic = semanticContext.validate(statement);
        if (!semantic.isSuccess()) return CommandResult.failure(semantic.diagnostics());
        progress.report("Analyzing statement");
        pipeline
            .staticAnalyzer()
            .analyze(
                statement,
                semantic.semanticModel(),
                config,
                diagnostic -> {
                  diagnosticSink.accept(diagnostic);
                  diagnosticCount.incrementAndGet();
                  if (diagnostic.severity() == Severity.ERROR) {
                    errorCount.incrementAndGet();
                  }
                });
        semanticContext = semantic.nextContext();
      }
      return CommandResult.success(new AnalysisResult(diagnosticCount.get(), errorCount.get()));
    } catch (SyntaxException exception) {
      return CommandResult.failure(List.of(exception.diagnostic()));
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
    SemanticContext semanticContext =
        SemanticContext.empty(
            pipeline.builtins(), pipeline.typeAnnotations(), pipeline.binaryOperatorRules());
    try {
      progress.report(READING_STATEMENTS);
      StatementSource statements =
          new StatementSyntaxReader(new Lexer(source, pipeline.keywords()));
      while (statements.hasNext()) {
        SemanticStatementResult semantic = semanticContext.validate(statements.next());
        if (!semantic.isSuccess()) return CommandResult.failure(semantic.diagnostics());
        semanticContext = semantic.nextContext();
      }
      return CommandResult.success(null);
    } catch (SyntaxException exception) {
      return CommandResult.failure(List.of(exception.diagnostic()));
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
