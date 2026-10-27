package org.printscript.ast.nodes.statements;

import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.source.SourceSpan;

/** An expression used as a statement; {@link #span} includes the trailing semicolon. */
public record ExpressionStatementSyntax(ExpressionSyntax expression, SourceSpan span)
    implements StatementSyntax {
  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitExpressionStatement(this);
  }
}
