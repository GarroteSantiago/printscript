package org.printscript.syntax.nodes.statements;

import org.printscript.source.SourceSpan;
import org.printscript.syntax.nodes.expressions.ExpressionSyntax;
import org.printscript.tokens.SyntaxToken;

public record ExpressionStatementSyntax(ExpressionSyntax expression, SyntaxToken semicolon)
    implements StatementSyntax {
  @Override
  public SourceSpan span() {
    return new SourceSpan(expression.span().start(), semicolon.span().end());
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitExpressionStatement(this);
  }
}
