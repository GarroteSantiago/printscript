package org.printscript.ast.nodes.statements;

import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.source.SourceSpan;

/** {@code name = value;} — {@link #span} covers the whole statement, semicolon included. */
public record AssignmentSyntax(String name, ExpressionSyntax value, SourceSpan span)
    implements StatementSyntax {
  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitAssignment(this);
  }
}
