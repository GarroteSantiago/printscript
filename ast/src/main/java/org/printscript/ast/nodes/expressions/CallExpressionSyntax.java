package org.printscript.ast.nodes.expressions;

import java.util.List;
import org.printscript.source.SourceSpan;
import org.printscript.tokens.SyntaxToken;

public record CallExpressionSyntax(
    SyntaxToken callee,
    SyntaxToken leftParen,
    List<ExpressionSyntax> arguments,
    SyntaxToken rightParen)
    implements ExpressionSyntax {
  public CallExpressionSyntax {
    arguments = List.copyOf(arguments);
  }

  @Override
  public SourceSpan span() {
    return new SourceSpan(callee.span().start(), rightParen.span().end());
  }

  @Override
  public <R> R accept(ExpressionVisitor<R> visitor) {
    return visitor.visitCall(this);
  }
}
