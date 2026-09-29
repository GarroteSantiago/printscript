package org.printscript.formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.printscript.ast.StatementSource;
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
import org.printscript.tokens.SyntaxToken;
import org.printscript.tokens.TokenType;

/**
 * Rewrites source by flattening a statement into its tokens (each tagged with block-nesting {@code
 * depth}) and rewriting only the leading trivia {@link SpacingRules} has an opinion about; every
 * other character — including comments and untouched whitespace — is re-emitted byte for byte. This
 * is a targeted trivia rewrite, not a pretty-printer: it never reconstructs source from the AST's
 * structure alone.
 *
 * <p>{@link #newSession} starts a stateful {@link Session} that must see every statement of a
 * program in order (it tracks the previous token and whether the previous statement was a {@code
 * println}, both needed to decide the next statement's leading trivia) and must be finished with
 * {@link Session#finish} to flush the trailing trivia before the EOF token. {@link
 * #format(ProgramSyntax, FormatterConfigProvider)} is a convenience that drives a session over an
 * already-built tree; {@code application.PrintScript}'s production path instead opens a session and
 * feeds it statements as they stream off a {@code StatementSource}.
 */
public final class PrintScriptFormatter {
  private final SpacingRules spacingRules;

  public PrintScriptFormatter() {
    this(SpacingRules.v1());
  }

  public PrintScriptFormatter(SpacingRules spacingRules) {
    this.spacingRules = spacingRules;
  }

  public Session newSession(FormatterConfigProvider config) {
    return new Session(config);
  }

  /**
   * Drains a {@link StatementSource}, formatting each statement in turn into {@code out} and
   * finishing with the source's trailing {@code eof} trivia. This is the production path's
   * counterpart to {@link #format(ProgramSyntax, FormatterConfigProvider)}, which drives a session
   * over an already-built tree instead. Formatting never validates — a syntactically valid but
   * semantically invalid program still formats — so this takes a plain {@link StatementSource}, not
   * a {@code typetable.ValidatedStatementSource}.
   */
  public void formatAll(StatementSource statements, FormatterConfigProvider config, Appendable out)
      throws IOException {
    Session session = newSession(config);
    while (statements.hasNext()) {
      session.format(statements.next(), out);
    }
    session.finish(statements.eof(), out);
  }

  public String format(ProgramSyntax program, FormatterConfigProvider config) {
    Session session = newSession(config);
    StringBuilder out = new StringBuilder();
    for (StatementSyntax statement : program.statements()) {
      out.append(session.format(statement));
    }
    out.append(session.finish(program.eof()));
    return out.toString();
  }

  public final class Session {
    private final FormatterConfigProvider config;
    private SyntaxToken previous;
    private boolean previousStatementIsPrintln;

    private Session(FormatterConfigProvider config) {
      this.config = config;
    }

    public String format(StatementSyntax statement) {
      StringBuilder out = new StringBuilder();
      try {
        format(statement, out);
      } catch (IOException exception) {
        throw new IllegalStateException("StringBuilder append failed", exception);
      }
      return out.toString();
    }

    public void format(StatementSyntax statement, Appendable out) throws IOException {
      boolean previousWasPrintln = previousStatementIsPrintln;
      for (PositionedToken positioned : flatten(statement)) {
        SyntaxToken token = positioned.token();
        String leadingTrivia = rewriteLeadingTrivia(previous, token, config, previousWasPrintln);
        String trivia = leadingTrivia;
        if (positioned.depth() > 0 && hasLineBreak(trivia)) {
          trivia =
              config
                  .blockIndentSpaces()
                  .map(spaces -> reindent(leadingTrivia, positioned.depth(), spaces))
                  .orElse(trivia);
        }
        out.append(trivia);
        out.append(token.text());
        previous = token;
      }
      previousStatementIsPrintln = isPrintlnStatement(statement);
    }

    public String finish(SyntaxToken eof) {
      return rewriteTrailingTrivia(previous, eof, config);
    }

    public void finish(SyntaxToken eof, Appendable out) throws IOException {
      out.append(finish(eof));
    }
  }

  private record PositionedToken(SyntaxToken token, int depth) {}

  private List<PositionedToken> flatten(StatementSyntax statement) {
    List<PositionedToken> tokens = new ArrayList<>();
    addStatement(statement, tokens, 0);
    return List.copyOf(tokens);
  }

  private void addStatement(StatementSyntax statement, List<PositionedToken> tokens, int depth) {
    statement.accept(new StatementFlattener(tokens, depth));
  }

  private final class StatementFlattener implements StatementVisitor<Void> {
    private final List<PositionedToken> tokens;
    private final int depth;

    StatementFlattener(List<PositionedToken> tokens, int depth) {
      this.tokens = tokens;
      this.depth = depth;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclarationSyntax declaration) {
      tokens.add(new PositionedToken(declaration.keyword(), depth));
      tokens.add(new PositionedToken(declaration.identifier(), depth));
      tokens.add(new PositionedToken(declaration.colon(), depth));
      tokens.add(new PositionedToken(declaration.type(), depth));
      if (declaration.equals().isPresent()) {
        tokens.add(new PositionedToken(declaration.equals().get(), depth));
        addExpression(declaration.initializer().orElseThrow(), tokens, depth);
      }
      tokens.add(new PositionedToken(declaration.semicolon(), depth));
      return null;
    }

    @Override
    public Void visitAssignment(AssignmentSyntax assignment) {
      tokens.add(new PositionedToken(assignment.identifier(), depth));
      tokens.add(new PositionedToken(assignment.equals(), depth));
      addExpression(assignment.value(), tokens, depth);
      tokens.add(new PositionedToken(assignment.semicolon(), depth));
      return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementSyntax expressionStatement) {
      addExpression(expressionStatement.expression(), tokens, depth);
      tokens.add(new PositionedToken(expressionStatement.semicolon(), depth));
      return null;
    }

    @Override
    public Void visitIf(IfStatementSyntax ifStatement) {
      tokens.add(new PositionedToken(ifStatement.ifKeyword(), depth));
      tokens.add(new PositionedToken(ifStatement.leftParen(), depth));
      addExpression(ifStatement.condition(), tokens, depth);
      tokens.add(new PositionedToken(ifStatement.rightParen(), depth));
      addBlock(ifStatement.thenBlock(), tokens, depth);
      if (ifStatement.elseKeyword().isPresent()) {
        tokens.add(new PositionedToken(ifStatement.elseKeyword().get(), depth));
        addBlock(ifStatement.elseBlock().orElseThrow(), tokens, depth);
      }
      return null;
    }

    @Override
    public Void visitBlock(BlockStatementSyntax block) {
      addBlock(block, tokens, depth);
      return null;
    }
  }

  private void addBlock(BlockStatementSyntax block, List<PositionedToken> tokens, int depth) {
    tokens.add(new PositionedToken(block.leftBrace(), depth));
    for (StatementSyntax inner : block.statements()) {
      addStatement(inner, tokens, depth + 1);
    }
    tokens.add(new PositionedToken(block.rightBrace(), depth));
  }

  private void addExpression(ExpressionSyntax expression, List<PositionedToken> tokens, int depth) {
    expression.accept(new ExpressionFlattener(tokens, depth));
  }

  private final class ExpressionFlattener implements ExpressionVisitor<Void> {
    private final List<PositionedToken> tokens;
    private final int depth;

    ExpressionFlattener(List<PositionedToken> tokens, int depth) {
      this.tokens = tokens;
      this.depth = depth;
    }

    @Override
    public Void visitLiteral(LiteralExpressionSyntax literal) {
      tokens.add(new PositionedToken(literal.literal(), depth));
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierExpressionSyntax identifier) {
      tokens.add(new PositionedToken(identifier.identifier(), depth));
      return null;
    }

    @Override
    public Void visitBinary(BinaryExpressionSyntax binary) {
      addExpression(binary.left(), tokens, depth);
      tokens.add(new PositionedToken(binary.operator(), depth));
      addExpression(binary.right(), tokens, depth);
      return null;
    }

    @Override
    public Void visitCall(CallExpressionSyntax call) {
      tokens.add(new PositionedToken(call.callee(), depth));
      tokens.add(new PositionedToken(call.leftParen(), depth));
      for (ExpressionSyntax argument : call.arguments()) {
        addExpression(argument, tokens, depth);
      }
      tokens.add(new PositionedToken(call.rightParen(), depth));
      return null;
    }
  }

  private String reindent(String trivia, int depth, int blockIndentSpaces) {
    int lastNewline = trivia.lastIndexOf('\n');
    return trivia.substring(0, lastNewline + 1) + " ".repeat(depth * blockIndentSpaces);
  }

  private String rewriteLeadingTrivia(
      SyntaxToken previous,
      SyntaxToken token,
      FormatterConfigProvider config,
      boolean previousStatementIsPrintln) {
    if (previous == null) {
      return token.leadingTrivia();
    }
    if (containsComment(token.leadingTrivia())) {
      if (TokenType.SEMICOLON.equals(previous.type())) {
        return config
            .spacesAfterSemicolon()
            .map(
                spaces ->
                    rewriteWhitespaceBeforeFirstComment(token.leadingTrivia(), " ".repeat(spaces)))
            .orElse(token.leadingTrivia());
      }
      return token.leadingTrivia();
    }
    if (previousStatementIsPrintln && TokenType.SEMICOLON.equals(previous.type())) {
      Optional<String> blankLines = config.blankLinesBeforePrintln().map(n -> "\n".repeat(n + 1));
      if (blankLines.isPresent()) {
        return blankLines.get();
      }
    }
    return spacingRules.leadingTriviaFor(token, previous, config).orElse(token.leadingTrivia());
  }

  private String rewriteTrailingTrivia(
      SyntaxToken previous, SyntaxToken eof, FormatterConfigProvider config) {
    if (previous == null || containsComment(eof.leadingTrivia())) {
      return eof.leadingTrivia();
    }
    if (TokenType.SEMICOLON.equals(previous.type()) && !hasLineBreak(eof.leadingTrivia())) {
      return config
          .spacesAfterSemicolon()
          .map(spaces -> "\n" + " ".repeat(spaces))
          .orElse(eof.leadingTrivia());
    }
    return eof.leadingTrivia();
  }

  private boolean isPrintlnStatement(StatementSyntax statement) {
    if (!(statement instanceof ExpressionStatementSyntax expressionStatement)) {
      return false;
    }
    if (!(expressionStatement.expression() instanceof CallExpressionSyntax call)) {
      return false;
    }
    return "println".equals(call.callee().semanticLexeme());
  }

  private boolean containsComment(String trivia) {
    return trivia.indexOf('#') >= 0;
  }

  private boolean hasLineBreak(String trivia) {
    return trivia.indexOf('\n') >= 0;
  }

  private String rewriteWhitespaceBeforeFirstComment(String trivia, String replacement) {
    int comment = trivia.indexOf('#');
    if (comment < 0) return trivia;
    return replacement + trivia.substring(comment);
  }
}
