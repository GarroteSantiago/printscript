package org.printscript.ast.nodes.statements;

import java.util.Optional;
import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.source.SourceSpan;
import org.printscript.tokens.SyntaxToken;
import org.printscript.tokens.TokenType;

public record VariableDeclarationSyntax(
    SyntaxToken keyword,
    SyntaxToken identifier,
    SyntaxToken colon,
    SyntaxToken type,
    Optional<SyntaxToken> equals,
    Optional<ExpressionSyntax> initializer,
    SyntaxToken semicolon)
    implements StatementSyntax {
  public boolean isConst() {
    return TokenType.CONST.equals(keyword.type());
  }

  @Override
  public SourceSpan span() {
    return new SourceSpan(keyword.span().start(), semicolon.span().end());
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitVariableDeclaration(this);
  }
}
