package org.printscript.ast.nodes.statements;

import java.util.Optional;
import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.source.SourceSpan;

/**
 * {@code let|const name: type [= initializer];}. {@link #typeAnnotation} is the annotation as
 * written — resolving it to a type is version-specific, so it's the typechecker's job, not the
 * parser's. {@link #nameSpan} locates just the name (for diagnostics about it); {@link #span}
 * covers the whole statement, semicolon included.
 */
public record VariableDeclarationSyntax(
    boolean isConst,
    String name,
    SourceSpan nameSpan,
    String typeAnnotation,
    Optional<ExpressionSyntax> initializer,
    SourceSpan span)
    implements StatementSyntax {
  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitVariableDeclaration(this);
  }
}
