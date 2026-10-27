package org.printscript.ast.nodes;

import java.util.List;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;

/**
 * A whole program: its statements, in order. {@link #span} runs from the first statement's start to
 * the last one's end; an empty program has an empty span at the start of the source.
 */
public record ProgramSyntax(List<StatementSyntax> statements) implements SyntaxNode {
  private static final SourceSpan START_OF_SOURCE = SourceSpan.at(new SourcePosition(1, 1, 0));

  public ProgramSyntax {
    statements = List.copyOf(statements);
  }

  @Override
  public SourceSpan span() {
    if (statements.isEmpty()) {
      return START_OF_SOURCE;
    }
    return new SourceSpan(statements.getFirst().span().start(), statements.getLast().span().end());
  }
}
