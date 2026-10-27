package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.ast.nodes.expressions.BinaryOperator;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.statements.AssignmentSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.testkit.TestSources;

class ParserTest {
  private static final String SOURCE =
      """
          let a: number = 12;
          let b: number = 4;
          a = a / b + 1;
          println("Result: " + a);
          """;

  private List<StatementSyntax> parsed;
  private AssignmentSyntax assignment;
  private BinaryExpressionSyntax plus;
  private BinaryExpressionSyntax divide;
  private ExpressionStatementSyntax callStatement;

  @BeforeEach
  public void parseSource() {
    parsed = TestSources.statementListOf(SOURCE);
    assignment = (AssignmentSyntax) parsed.get(2);
    plus =
        assertInstanceOf(
            BinaryExpressionSyntax.class, assignment.value(), "expected a binary expression");
    divide =
        assertInstanceOf(
            BinaryExpressionSyntax.class, plus.left(), "expected a nested binary expression");
    callStatement =
        assertInstanceOf(
            ExpressionStatementSyntax.class, parsed.get(3), "expected an expression statement");
  }

  @Test
  public void parsesExpectedStatementCount() {
    assertEquals(4, parsed.size(), "expected 4 statements");
  }

  @Test
  public void parsesFirstStatementAsVariableDeclaration() {
    assertInstanceOf(
        VariableDeclarationSyntax.class, parsed.get(0), "expected a variable declaration");
  }

  @Test
  public void parsesThirdStatementAsAssignment() {
    assertInstanceOf(AssignmentSyntax.class, parsed.get(2), "expected an assignment");
  }

  @Test
  public void parsesAdditionAsTopLevelBinaryOperator() {
    assertEquals(BinaryOperator.ADD, plus.operator(), "expected top-level '+' operator");
  }

  @Test
  public void parsesDivisionAsHigherPrecedenceThanAddition() {
    assertEquals(BinaryOperator.DIVIDE, divide.operator(), "expected nested '/' operator");
  }

  @Test
  public void parsesFourthStatementExpressionAsCall() {
    assertInstanceOf(
        CallExpressionSyntax.class, callStatement.expression(), "expected a call expression");
  }
}
