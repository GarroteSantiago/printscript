package org.printscript.ast.nodes.expressions;

import org.printscript.source.SourceSpan;
import org.printscript.tokens.SyntaxToken;
import org.printscript.types.TypeName;

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
