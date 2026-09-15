package org.printscript.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.lexer.KeywordTable;
import org.printscript.syntax.nodes.statements.VariableDeclarationSyntax;

class TestSourcesTest {
  @Test
  void statementsOfParsesAStatementWithDefaultKeywords() {
    var statements = TestSources.statementsOf("let x: number = 1;");

    assertTrue(statements.hasNext(), "expected a parsed statement");
  }

  @Test
  void statementsOfWithKeywordsRecognizesV11OnlyKeywords() {
    var statement =
        (VariableDeclarationSyntax)
            TestSources.statementsOf("const x: number = 1;", KeywordTable.v1_1()).next();

    assertTrue(statement.isConst(), "expected const to be recognized under the v1.1 keywords");
  }

  @Test
  void programOfBuildsAProgramWithEveryStatement() {
    var program = TestSources.programOf("let x: number = 1;\nlet y: number = 2;");

    assertEquals(2, program.statements().size(), "expected both statements in the program");
  }

  @Test
  void programOfWithKeywordsBuildsAProgramUsingThoseKeywords() {
    var program = TestSources.programOf("const x: number = 1;", KeywordTable.v1_1());

    assertEquals(1, program.statements().size(), "expected the single statement in the program");
  }
}
