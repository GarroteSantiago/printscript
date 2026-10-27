package org.printscript.testkit;

import java.util.ArrayList;
import java.util.List;
import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.lexer.KeywordTable;
import org.printscript.lexer.Lexer;
import org.printscript.parser.StatementSyntaxReader;
import org.printscript.tokens.TokenSource;

/**
 * Test-only helper that centralizes "turn this string into pipeline data" — the {@code new
 * StatementSyntaxReader(new Lexer(source))} wiring every test fixture across the repo otherwise
 * needs. This is the one place outside the {@code toolchain} composition root allowed to construct
 * a concrete {@code Lexer}; keep it that way rather than letting a production module depend on
 * {@code lexer} just to build test data.
 */
public final class TestSources {
  private TestSources() {}

  public static TokenSource tokensOf(String source) {
    return new Lexer(source);
  }

  public static TokenSource tokensOf(String source, KeywordTable keywords) {
    return new Lexer(source, keywords);
  }

  public static StatementSource statementsOf(String source) {
    return new StatementSyntaxReader(new Lexer(source));
  }

  public static StatementSource statementsOf(String source, KeywordTable keywords) {
    return new StatementSyntaxReader(new Lexer(source, keywords));
  }

  /** Every statement of {@code source}, parsed with the v1.0 keywords. */
  public static List<StatementSyntax> statementListOf(String source) {
    return drain(statementsOf(source));
  }

  public static List<StatementSyntax> statementListOf(String source, KeywordTable keywords) {
    return drain(statementsOf(source, keywords));
  }

  private static List<StatementSyntax> drain(StatementSource statements) {
    List<StatementSyntax> result = new ArrayList<>();
    while (statements.hasNext()) {
      result.add(statements.next());
    }
    return List.copyOf(result);
  }
}
