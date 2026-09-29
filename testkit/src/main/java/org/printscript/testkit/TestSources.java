package org.printscript.testkit;

import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.ProgramSyntax;
import org.printscript.lexer.KeywordTable;
import org.printscript.lexer.Lexer;
import org.printscript.parser.StatementSyntaxReader;
import org.printscript.parser.SyntaxTreeBuilder;

/**
 * Test-only helper that centralizes "turn this string into pipeline data" — the {@code new
 * StatementSyntaxReader(new Lexer(source))} wiring every test fixture across the repo otherwise
 * needs. This is the one place outside the {@code toolchain} composition root allowed to construct
 * a concrete {@code Lexer}; keep it that way rather than letting a production module depend on
 * {@code lexer} just to build test data.
 */
public final class TestSources {
  private TestSources() {}

  public static StatementSource statementsOf(String source) {
    return new StatementSyntaxReader(new Lexer(source));
  }

  public static StatementSource statementsOf(String source, KeywordTable keywords) {
    return new StatementSyntaxReader(new Lexer(source, keywords));
  }

  public static ProgramSyntax programOf(String source) {
    return new SyntaxTreeBuilder(statementsOf(source)).buildProgram();
  }

  public static ProgramSyntax programOf(String source, KeywordTable keywords) {
    return new SyntaxTreeBuilder(statementsOf(source, keywords)).buildProgram();
  }
}
