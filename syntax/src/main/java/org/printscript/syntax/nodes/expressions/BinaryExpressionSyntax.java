package org.printscript.syntax.nodes.expressions;

import org.printscript.source.SourceSpan;
import org.printscript.tokens.SyntaxToken;

public record BinaryExpressionSyntax(
    ExpressionSyntax left, SyntaxToken operator, ExpressionSyntax right)
    implements ExpressionSyntax {
  @Override
  public SourceSpan span() {
    return new SourceSpan(left.span().start(), right.span().end());
  }

  @Override
  public <R> R accept(ExpressionVisitor<R> visitor) {
    return visitor.visitBinary(this);
  }
}
