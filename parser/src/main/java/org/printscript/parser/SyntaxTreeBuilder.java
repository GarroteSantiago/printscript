package org.printscript.parser;

import java.util.ArrayList;
import java.util.List;
import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.ProgramSyntax;
import org.printscript.ast.nodes.statements.StatementSyntax;

/**
 * Drains a {@link StatementSource} into a fully materialized {@link ProgramSyntax}. The production
 * commands in {@code application.PrintScript} do not use this — they stream statement-by-statement
 * directly off the {@link StatementSource} so semantic validation and execution/formatting/analysis
 * interleave per statement. This class exists for callers that genuinely want the whole tree at
 * once, which today is only {@code testkit.TestSources#programOf} and this module's own tests.
 */
public final class SyntaxTreeBuilder {
  private final StatementSource statements;

  public SyntaxTreeBuilder(StatementSource statements) {
    this.statements = statements;
  }

  public ProgramSyntax buildProgram() {
    List<StatementSyntax> result = new ArrayList<>();
    while (statements.hasNext()) result.add(statements.next());
    return new ProgramSyntax(result, statements.eof());
  }
}
