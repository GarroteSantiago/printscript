package org.printscript.ast.nodes.statements;

import java.util.List;
import org.printscript.source.SourceSpan;

/** {@code { ... }} — {@link #span} runs from the opening to the closing brace. */
public record BlockStatementSyntax(List<StatementSyntax> statements, SourceSpan span)
    implements StatementSyntax {
  public BlockStatementSyntax {
    statements = List.copyOf(statements);
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitBlock(this);
  }
}
