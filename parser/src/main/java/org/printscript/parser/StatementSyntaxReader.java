package org.printscript.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.ast.nodes.expressions.BinaryOperator;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.ast.nodes.expressions.IdentifierExpressionSyntax;
import org.printscript.ast.nodes.expressions.LiteralExpressionSyntax;
import org.printscript.ast.nodes.statements.AssignmentSyntax;
import org.printscript.ast.nodes.statements.BlockStatementSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.IfStatementSyntax;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.DiagnosticCode;
import org.printscript.diagnostics.DiagnosticException;
import org.printscript.diagnostics.Phase;
import org.printscript.source.SourceSpan;
import org.printscript.tokens.Token;
import org.printscript.tokens.TokenSource;
import org.printscript.tokens.TokenType;
import org.printscript.types.TypeName;

/**
 * Recursive-descent parser: the sole production implementation of {@link StatementSource}. Pulls
 * from a {@link TokenSource} — never constructs a {@code Lexer} itself, so this class works against
 * any token producer — and keeps a one-token lookahead ({@code current}/{@code next}) to
 * disambiguate an assignment (`identifier =`) from an expression statement. Expression parsing
 * implements a fixed two-level precedence (addition/subtraction over multiplication/division; no
 * further operators exist in this grammar). There is no error recovery: the first malformed
 * construct throws a {@link DiagnosticException} and parsing stops. An error raised because the
 * input ended mid-construct carries {@link DiagnosticCode#UNEXPECTED_END_OF_INPUT}, so a caller
 * reading input incrementally (the REPL) can tell "needs more input" from "is wrong".
 */
public final class StatementSyntaxReader implements StatementSource {
  private final TokenSource tokenSource;
  private Token current;
  private Token next;
  private Token previous;

  public StatementSyntaxReader(TokenSource tokenSource) {
    this.tokenSource = tokenSource;
    this.current = tokenSource.next();
    this.next = tokenSource.next();
  }

  @Override
  public boolean hasNext() {
    return !check(TokenType.EOF);
  }

  @Override
  public StatementSyntax next() {
    if (!hasNext()) {
      throw error(current, "Expected statement");
    }
    return statement();
  }

  private StatementSyntax statement() {
    if (match(TokenType.LET, TokenType.CONST)) {
      return variableDeclaration(previous);
    }
    if (match(TokenType.IF)) {
      return ifStatement(previous);
    }
    if (check(TokenType.IDENTIFIER) && checkNext(TokenType.EQUAL)) {
      return assignment();
    }
    ExpressionSyntax expression = expression();
    Token semicolon = consume(TokenType.SEMICOLON, "Expected ';' after statement");
    return new ExpressionStatementSyntax(expression, spanning(expression.span(), semicolon));
  }

  private StatementSyntax variableDeclaration(Token keyword) {
    Token name = consume(TokenType.IDENTIFIER, "Expected variable name");
    consume(TokenType.COLON, "Expected ':' after variable name");
    Token type = consume(TokenType.TYPE, "Expected type annotation");
    Optional<ExpressionSyntax> initializer = Optional.empty();
    if (match(TokenType.EQUAL)) {
      initializer = Optional.of(expression());
    }
    Token semicolon = consume(TokenType.SEMICOLON, "Expected ';' after declaration");
    return new VariableDeclarationSyntax(
        TokenType.CONST.equals(keyword.type()),
        name.semanticLexeme(),
        name.span(),
        type.semanticLexeme(),
        initializer,
        spanning(keyword.span(), semicolon));
  }

  private StatementSyntax ifStatement(Token ifKeyword) {
    consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'");
    ExpressionSyntax condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition");
    BlockStatementSyntax thenBlock = block();
    Optional<BlockStatementSyntax> elseBlock = Optional.empty();
    if (match(TokenType.ELSE)) {
      elseBlock = Optional.of(block());
    }
    SourceSpan last = elseBlock.orElse(thenBlock).span();
    return new IfStatementSyntax(
        condition, thenBlock, elseBlock, new SourceSpan(ifKeyword.span().start(), last.end()));
  }

  private BlockStatementSyntax block() {
    Token leftBrace = consume(TokenType.LEFT_BRACE, "Expected '{' to start block");
    List<StatementSyntax> statements = new ArrayList<>();
    while (!check(TokenType.RIGHT_BRACE)) {
      statements.add(statement());
    }
    Token rightBrace = consume(TokenType.RIGHT_BRACE, "Expected '}' to close block");
    return new BlockStatementSyntax(statements, spanning(leftBrace.span(), rightBrace));
  }

  private StatementSyntax assignment() {
    Token name = consume(TokenType.IDENTIFIER, "Expected variable name");
    consume(TokenType.EQUAL, "Expected '=' after variable name");
    ExpressionSyntax value = expression();
    Token semicolon = consume(TokenType.SEMICOLON, "Expected ';' after assignment");
    return new AssignmentSyntax(name.semanticLexeme(), value, spanning(name.span(), semicolon));
  }

  private ExpressionSyntax expression() {
    return addition();
  }

  private ExpressionSyntax addition() {
    ExpressionSyntax expression = multiplication();
    while (match(TokenType.PLUS, TokenType.MINUS)) {
      BinaryOperator operator =
          TokenType.PLUS.equals(previous.type()) ? BinaryOperator.ADD : BinaryOperator.SUBTRACT;
      ExpressionSyntax right = multiplication();
      expression = new BinaryExpressionSyntax(expression, operator, right);
    }
    return expression;
  }

  private ExpressionSyntax multiplication() {
    ExpressionSyntax expression = primary();
    while (match(TokenType.STAR, TokenType.SLASH)) {
      BinaryOperator operator =
          TokenType.STAR.equals(previous.type()) ? BinaryOperator.MULTIPLY : BinaryOperator.DIVIDE;
      ExpressionSyntax right = primary();
      expression = new BinaryExpressionSyntax(expression, operator, right);
    }
    return expression;
  }

  private ExpressionSyntax primary() {
    if (match(TokenType.NUMBER)) {
      return literal(TypeName.NUMBER);
    }
    if (match(TokenType.STRING)) {
      return literal(TypeName.STRING);
    }
    if (match(TokenType.BOOLEAN)) {
      return literal(TypeName.BOOLEAN);
    }
    if (match(TokenType.IDENTIFIER)) {
      Token identifier = previous;
      if (match(TokenType.LEFT_PAREN)) {
        List<ExpressionSyntax> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
          arguments.add(expression());
        }
        Token rightParen = consume(TokenType.RIGHT_PAREN, "Expected ')' after call arguments");
        return new CallExpressionSyntax(
            identifier.semanticLexeme(), arguments, spanning(identifier.span(), rightParen));
      }
      return new IdentifierExpressionSyntax(identifier.semanticLexeme(), identifier.span());
    }
    throw error(current, "Expected expression");
  }

  private LiteralExpressionSyntax literal(TypeName type) {
    return new LiteralExpressionSyntax(previous.semanticLexeme(), type, previous.span());
  }

  /**
   * From the start of {@code first} to the end of {@code last} — how every node's span is built.
   */
  private static SourceSpan spanning(SourceSpan first, Token last) {
    return new SourceSpan(first.start(), last.span().end());
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw error(current, message);
  }

  private boolean check(TokenType type) {
    return current.type().equals(type);
  }

  private boolean checkNext(TokenType type) {
    return next.type().equals(type);
  }

  private Token advance() {
    previous = current;
    current = next;
    next = tokenSource.next();
    return previous;
  }

  private DiagnosticException error(Token token, String message) {
    Diagnostic diagnostic = Diagnostic.error(Phase.SYNTAX, message, token.span());
    if (token.type() == TokenType.EOF) {
      diagnostic = diagnostic.withCode(DiagnosticCode.UNEXPECTED_END_OF_INPUT);
    }
    return new DiagnosticException(diagnostic);
  }
}
