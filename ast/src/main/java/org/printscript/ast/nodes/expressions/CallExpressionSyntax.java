package org.printscript.ast.nodes.expressions;

import java.util.List;
import org.printscript.source.SourceSpan;

/** A call such as {@code println(x)}; {@link #span} runs from the callee to the closing paren. */
public record CallExpressionSyntax(String callee, List<ExpressionSyntax> arguments, SourceSpan span)
    implements ExpressionSyntax {
  public CallExpressionSyntax {
    arguments = List.copyOf(arguments);
  }

  @Override
  public <R> R accept(ExpressionVisitor<R> visitor) {
    return visitor.visitCall(this);
  }
}
