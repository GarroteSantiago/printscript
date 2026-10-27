package org.printscript.ast.nodes.expressions;

import org.printscript.source.SourceSpan;

public record IdentifierExpressionSyntax(String name, SourceSpan span) implements ExpressionSyntax {
  @Override
  public <R> R accept(ExpressionVisitor<R> visitor) {
    return visitor.visitIdentifier(this);
  }
}
