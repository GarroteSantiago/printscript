package org.printscript.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.lexer.KeywordTable;
import org.printscript.tokens.TokenType;

class TestSourcesTest {
  @Test
  public void statementsOfParsesAStatementWithDefaultKeywords() {
    var statements = TestSources.statementsOf("let x: number = 1;");

    assertTrue(statements.hasNext(), "expected a parsed statement");
  }

  @Test
  public void statementsOfWithKeywordsRecognizesV11OnlyKeywords() {
    var statement =
        (VariableDeclarationSyntax)
            TestSources.statementsOf("const x: number = 1;", KeywordTable.v1_1()).next();

    assertTrue(statement.isConst(), "expected const to be recognized under the v1.1 keywords");
  }

  @Test
  public void statementListOfReturnsEveryStatement() {
    var parsed = TestSources.statementListOf("let x: number = 1;\nlet y: number = 2;");

    assertEquals(2, parsed.size(), "expected both statements in the list");
  }

  @Test
  public void statementListOfWithKeywordsParsesUsingThoseKeywords() {
    var parsed = TestSources.statementListOf("const x: number = 1;", KeywordTable.v1_1());

    assertEquals(1, parsed.size(), "expected the single statement in the list");
  }

  @Test
  public void tokensOfLexesWithDefaultKeywords() {
    assertEquals(
        TokenType.LET,
        TestSources.tokensOf("let x: number = 1;").next().type(),
        "expected the first token to be 'let'");
  }

  @Test
  public void tokensOfWithKeywordsRecognizesV11OnlyKeywords() {
    assertEquals(
        TokenType.CONST,
        TestSources.tokensOf("const x: number = 1;", KeywordTable.v1_1()).next().type(),
        "expected const to be a keyword under the v1.1 keywords");
  }
}
