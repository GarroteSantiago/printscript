package org.printscript.ast.nodes.statements;

import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.source.SourceSpan;
import org.printscript.tokens.SyntaxToken;

public record AssignmentSyntax(
    SyntaxToken identifier, SyntaxToken equals, ExpressionSyntax value, SyntaxToken semicolon)
    implements StatementSyntax {
  @Override
  public SourceSpan span() {
    return new SourceSpan(identifier.span().start(), semicolon.span().end());
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitAssignment(this);
  }
}
