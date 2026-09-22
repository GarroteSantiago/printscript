package org.printscript.syntax.nodes.statements;

import java.util.List;
import org.printscript.source.SourceSpan;
import org.printscript.tokens.SyntaxToken;

public record BlockStatementSyntax(
    SyntaxToken leftBrace, List<StatementSyntax> statements, SyntaxToken rightBrace)
    implements StatementSyntax {
  public BlockStatementSyntax {
    statements = List.copyOf(statements);
  }

  @Override
  public SourceSpan span() {
    return new SourceSpan(leftBrace.span().start(), rightBrace.span().end());
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitBlock(this);
  }
}
