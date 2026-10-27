package org.printscript.ast.nodes.expressions;

import org.printscript.source.SourceSpan;

public record BinaryExpressionSyntax(
    ExpressionSyntax left, BinaryOperator operator, ExpressionSyntax right)
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
