package org.printscript.ast.nodes.statements;

import java.util.Optional;
import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.source.SourceSpan;

/** {@code if (condition) { ... } else { ... }}; {@link #span} ends with the last block. */
public record IfStatementSyntax(
    ExpressionSyntax condition,
    BlockStatementSyntax thenBlock,
    Optional<BlockStatementSyntax> elseBlock,
    SourceSpan span)
    implements StatementSyntax {
  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitIf(this);
  }
}
