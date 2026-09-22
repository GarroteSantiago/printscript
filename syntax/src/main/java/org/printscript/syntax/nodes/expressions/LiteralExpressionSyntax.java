package org.printscript.syntax.nodes.expressions;

import org.printscript.source.SourceSpan;
import org.printscript.syntax.TypeName;
import org.printscript.tokens.SyntaxToken;

public record LiteralExpressionSyntax(SyntaxToken literal, TypeName literalType)
    implements ExpressionSyntax {
  @Override
  public SourceSpan span() {
    return literal.span();
  }

  @Override
  public <R> R accept(ExpressionVisitor<R> visitor) {
    return visitor.visitLiteral(this);
  }
}
