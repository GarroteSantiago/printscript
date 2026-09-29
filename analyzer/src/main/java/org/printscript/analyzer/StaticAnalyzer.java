package org.printscript.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.printscript.ast.nodes.ProgramSyntax;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.ast.nodes.expressions.ExpressionVisitor;
import org.printscript.ast.nodes.expressions.IdentifierExpressionSyntax;
import org.printscript.ast.nodes.expressions.LiteralExpressionSyntax;
import org.printscript.ast.nodes.statements.AssignmentSyntax;
import org.printscript.ast.nodes.statements.BlockStatementSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.IfStatementSyntax;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.ast.nodes.statements.StatementVisitor;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Phase;
import org.printscript.typetable.BuiltinRegistry;
import org.printscript.typetable.SemanticModel;
import org.printscript.typetable.ValidatedStatement;
import org.printscript.typetable.ValidatedStatementSource;

/**
 * Runs style/policy checks over already-validated statements — identifier naming (via the swappable
 * {@link NamingStyleRules}, default {@link NamingStyleRules#v1()}) and argument-shape restrictions
 * on {@code println}/{@code readInput} calls. Deliberately separate from {@code typetable}: type
 * errors and undeclared variables are correctness, decided once and recorded in {@link
 * SemanticModel}; this class only judges style and policy on top of an already-valid program, and
 * reads {@link SemanticModel#resolveCall} rather than re-resolving which builtin a call targets.
 * All three {@code analyze} overloads walk the same statement; they differ only in how diagnostics
 * are collected (a returned list vs. a {@link Consumer} for streaming callers).
 */
public final class StaticAnalyzer {
  private final NamingStyleRules namingStyleRules;

  public StaticAnalyzer() {
    this(NamingStyleRules.v1());
  }

  public StaticAnalyzer(NamingStyleRules namingStyleRules) {
    this.namingStyleRules = namingStyleRules;
  }

  public List<Diagnostic> analyze(
      ProgramSyntax program, SemanticModel semanticModel, AnalyzerConfig config) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    for (StatementSyntax statement : program.statements()) {
      diagnostics.addAll(analyze(statement, semanticModel, config));
    }
    return List.copyOf(diagnostics);
  }

  public List<Diagnostic> analyze(
      StatementSyntax statement, SemanticModel semanticModel, AnalyzerConfig config) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    analyze(statement, semanticModel, config, diagnostics::add);
    return List.copyOf(diagnostics);
  }

  public void analyze(
      StatementSyntax statement,
      SemanticModel semanticModel,
      AnalyzerConfig config,
      Consumer<Diagnostic> diagnostics) {
    statement.accept(new StatementAnalyzer(semanticModel, config, diagnostics));
  }

  /**
   * Drains a {@link ValidatedStatementSource}, analyzing each statement in turn. Lets {@code
   * typetable.SemanticException} propagate rather than catching it, matching the other {@code
   * analyze} overloads' contract — there is no recovery, analysis stops at the first invalid
   * statement.
   */
  public void analyzeAll(
      ValidatedStatementSource statements, AnalyzerConfig config, Consumer<Diagnostic> sink) {
    while (statements.hasNext()) {
      ValidatedStatement validated = statements.next();
      analyze(validated.statement(), validated.model(), config, sink);
    }
  }

  private final class StatementAnalyzer implements StatementVisitor<Void> {
    private final SemanticModel semanticModel;
    private final AnalyzerConfig config;
    private final Consumer<Diagnostic> diagnostics;

    private StatementAnalyzer(
        SemanticModel semanticModel, AnalyzerConfig config, Consumer<Diagnostic> diagnostics) {
      this.semanticModel = semanticModel;
      this.config = config;
      this.diagnostics = diagnostics;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclarationSyntax declaration) {
      checkName(
          declaration.identifier().semanticLexeme(),
          declaration.identifier().span(),
          config,
          diagnostics);
      declaration
          .initializer()
          .ifPresent(initializer -> checkCalls(initializer, semanticModel, config, diagnostics));
      return null;
    }

    @Override
    public Void visitAssignment(AssignmentSyntax assignment) {
      checkCalls(assignment.value(), semanticModel, config, diagnostics);
      return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementSyntax expressionStatement) {
      checkCalls(expressionStatement.expression(), semanticModel, config, diagnostics);
      return null;
    }

    @Override
    public Void visitIf(IfStatementSyntax ifStatement) {
      checkCalls(ifStatement.condition(), semanticModel, config, diagnostics);
      for (StatementSyntax inner : ifStatement.thenBlock().statements()) {
        analyze(inner, semanticModel, config, diagnostics);
      }
      ifStatement
          .elseBlock()
          .ifPresent(
              elseBlock -> {
                for (StatementSyntax inner : elseBlock.statements()) {
                  analyze(inner, semanticModel, config, diagnostics);
                }
              });
      return null;
    }

    @Override
    public Void visitBlock(BlockStatementSyntax block) {
      for (StatementSyntax inner : block.statements()) {
        analyze(inner, semanticModel, config, diagnostics);
      }
      return null;
    }
  }

  private void checkCalls(
      ExpressionSyntax expression,
      SemanticModel semanticModel,
      AnalyzerConfig config,
      Consumer<Diagnostic> diagnostics) {
    List<CallExpressionSyntax> calls = new ArrayList<>();
    expression.accept(new CallCollector(calls));
    for (CallExpressionSyntax call : calls) {
      checkCallArgumentShape(
          call,
          BuiltinRegistry.PRINTLN,
          config.restrictPrintlnToSimpleArguments(),
          semanticModel,
          diagnostics);
      checkCallArgumentShape(
          call,
          BuiltinRegistry.READ_INPUT,
          config.restrictReadInputToSimpleArguments(),
          semanticModel,
          diagnostics);
    }
  }

  private static final class CallCollector implements ExpressionVisitor<Void> {
    private final List<CallExpressionSyntax> out;

    private CallCollector(List<CallExpressionSyntax> out) {
      this.out = out;
    }

    @Override
    public Void visitLiteral(LiteralExpressionSyntax literal) {
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierExpressionSyntax identifier) {
      return null;
    }

    @Override
    public Void visitBinary(BinaryExpressionSyntax binary) {
      binary.left().accept(this);
      binary.right().accept(this);
      return null;
    }

    @Override
    public Void visitCall(CallExpressionSyntax call) {
      out.add(call);
      for (ExpressionSyntax argument : call.arguments()) {
        argument.accept(this);
      }
      return null;
    }
  }

  private void checkCallArgumentShape(
      CallExpressionSyntax call,
      String builtinName,
      boolean enabled,
      SemanticModel semanticModel,
      Consumer<Diagnostic> diagnostics) {
    if (!enabled) return;
    if (semanticModel
            .resolveCall(call)
            .map(signature -> builtinName.equals(signature.name()))
            .orElse(false)
        && !call.arguments().isEmpty()
        && !(call.arguments().getFirst() instanceof IdentifierExpressionSyntax)
        && !(call.arguments().getFirst() instanceof LiteralExpressionSyntax)) {
      diagnostics.accept(
          error(
              builtinName + " argument must be an identifier or literal",
              call.arguments().getFirst().span()));
    }
  }

  private void checkName(
      String name,
      org.printscript.source.SourceSpan span,
      AnalyzerConfig config,
      Consumer<Diagnostic> diagnostics) {
    if (!config.checkIdentifierNaming()) return;
    boolean valid = namingStyleRules.matches(config.namingStyle(), name);
    if (!valid) {
      diagnostics.accept(
          error("Identifier '" + name + "' does not match " + config.namingStyle(), span));
    }
  }

  private Diagnostic error(String message, org.printscript.source.SourceSpan span) {
    return Diagnostic.error(Phase.ANALYZER, message, span);
  }
}
