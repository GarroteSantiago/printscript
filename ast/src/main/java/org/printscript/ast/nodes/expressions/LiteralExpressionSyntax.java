package org.printscript.ast.nodes.expressions;

import org.printscript.source.SourceSpan;
import org.printscript.types.TypeName;

/**
 * A literal value. {@link #value} is the literal's meaning as text — a string without its quotes, a
 * number's digits, {@code true}/{@code false} — and {@link #literalType} its type.
 */
public record LiteralExpressionSyntax(String value, TypeName literalType, SourceSpan span)
    implements ExpressionSyntax {
  @Override
  public <R> R accept(ExpressionVisitor<R> visitor) {
    return visitor.visitLiteral(this);
  }
}
