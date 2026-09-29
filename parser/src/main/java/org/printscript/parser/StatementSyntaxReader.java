package org.printscript.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
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
import org.printscript.diagnostics.Phase;
import org.printscript.tokens.SyntaxException;
import org.printscript.tokens.SyntaxToken;
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
 * construct throws a {@link org.printscript.tokens.SyntaxException} and parsing stops.
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

  @Override
  public SyntaxToken eof() {
    return syntax(current);
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
    return new ExpressionStatementSyntax(expression, syntax(semicolon));
  }

  private StatementSyntax variableDeclaration(Token keyword) {
    Token name = consume(TokenType.IDENTIFIER, "Expected variable name");
    Token colon = consume(TokenType.COLON, "Expected ':' after variable name");
    Token type = consume(TokenType.TYPE, "Expected type annotation");
    Optional<SyntaxToken> equals = Optional.empty();
    Optional<ExpressionSyntax> initializer = Optional.empty();
    if (match(TokenType.EQUAL)) {
      equals = Optional.of(syntax(previous));
      initializer = Optional.of(expression());
    }
    Token semicolon = consume(TokenType.SEMICOLON, "Expected ';' after declaration");
    return new VariableDeclarationSyntax(
        syntax(keyword),
        syntax(name),
        syntax(colon),
        syntax(type),
        equals,
        initializer,
        syntax(semicolon));
  }

  private StatementSyntax ifStatement(Token ifKeyword) {
    Token leftParen = consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'");
    ExpressionSyntax condition = expression();
    Token rightParen = consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition");
    BlockStatementSyntax thenBlock = block();
    Optional<SyntaxToken> elseKeyword = Optional.empty();
    Optional<BlockStatementSyntax> elseBlock = Optional.empty();
    if (match(TokenType.ELSE)) {
      elseKeyword = Optional.of(syntax(previous));
      elseBlock = Optional.of(block());
    }
    return new IfStatementSyntax(
        syntax(ifKeyword),
        syntax(leftParen),
        condition,
        syntax(rightParen),
        thenBlock,
        elseKeyword,
        elseBlock);
  }

  private BlockStatementSyntax block() {
    Token leftBrace = consume(TokenType.LEFT_BRACE, "Expected '{' to start block");
    List<StatementSyntax> statements = new ArrayList<>();
    while (!check(TokenType.RIGHT_BRACE)) {
      statements.add(statement());
    }
    Token rightBrace = consume(TokenType.RIGHT_BRACE, "Expected '}' to close block");
    return new BlockStatementSyntax(syntax(leftBrace), statements, syntax(rightBrace));
  }

  private StatementSyntax assignment() {
    Token name = consume(TokenType.IDENTIFIER, "Expected variable name");
    Token equals = consume(TokenType.EQUAL, "Expected '=' after variable name");
    ExpressionSyntax value = expression();
    Token semicolon = consume(TokenType.SEMICOLON, "Expected ';' after assignment");
    return new AssignmentSyntax(syntax(name), syntax(equals), value, syntax(semicolon));
  }

  private ExpressionSyntax expression() {
    return addition();
  }

  private ExpressionSyntax addition() {
    ExpressionSyntax expression = multiplication();
    while (match(TokenType.PLUS, TokenType.MINUS)) {
      Token operator = previous;
      ExpressionSyntax right = multiplication();
      expression = new BinaryExpressionSyntax(expression, syntax(operator), right);
    }
    return expression;
  }

  private ExpressionSyntax multiplication() {
    ExpressionSyntax expression = primary();
    while (match(TokenType.STAR, TokenType.SLASH)) {
      Token operator = previous;
      ExpressionSyntax right = primary();
      expression = new BinaryExpressionSyntax(expression, syntax(operator), right);
    }
    return expression;
  }

  private ExpressionSyntax primary() {
    if (match(TokenType.NUMBER)) {
      return new LiteralExpressionSyntax(syntax(previous), TypeName.NUMBER);
    }
    if (match(TokenType.STRING)) {
      return new LiteralExpressionSyntax(syntax(previous), TypeName.STRING);
    }
    if (match(TokenType.BOOLEAN)) {
      return new LiteralExpressionSyntax(syntax(previous), TypeName.BOOLEAN);
    }
    if (match(TokenType.IDENTIFIER)) {
      Token identifier = previous;
      if (match(TokenType.LEFT_PAREN)) {
        Token leftParen = previous;
        List<ExpressionSyntax> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
          arguments.add(expression());
        }
        Token rightParen = consume(TokenType.RIGHT_PAREN, "Expected ')' after call arguments");
        return new CallExpressionSyntax(
            syntax(identifier), syntax(leftParen), arguments, syntax(rightParen));
      }
      return new IdentifierExpressionSyntax(syntax(identifier));
    }
    throw error(current, "Expected expression");
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

  private SyntaxToken syntax(Token token) {
    return SyntaxToken.from(token);
  }

  private SyntaxException error(Token token, String message) {
    return new SyntaxException(Diagnostic.error(Phase.SYNTAX, message, token.span()));
  }
}
