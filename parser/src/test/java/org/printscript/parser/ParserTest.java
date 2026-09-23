package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.ProgramSyntax;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.statements.AssignmentSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.testkit.TestSources;
import org.printscript.tokens.TokenType;

class ParserTest {
  private static final String SOURCE =
      """
          let a: number = 12;
          let b: number = 4;
          a = a / b + 1;
          println("Result: " + a);
          """;

  private ProgramSyntax program;
  private AssignmentSyntax assignment;
  private BinaryExpressionSyntax plus;
  private BinaryExpressionSyntax divide;
  private ExpressionStatementSyntax callStatement;

  @BeforeEach
  void parseSource() {
    program = TestSources.programOf(SOURCE);
    assignment = (AssignmentSyntax) program.statements().get(2);
    plus =
        assertInstanceOf(
            BinaryExpressionSyntax.class, assignment.value(), "expected a binary expression");
    divide =
        assertInstanceOf(
            BinaryExpressionSyntax.class, plus.left(), "expected a nested binary expression");
    callStatement =
        assertInstanceOf(
            ExpressionStatementSyntax.class,
            program.statements().get(3),
            "expected an expression statement");
  }

  @Test
  void parsesExpectedStatementCount() {
    assertEquals(4, program.statements().size(), "expected 4 statements");
  }

  @Test
  void programSpanStartsAtFirstStatement() {
    assertEquals(
        program.statements().getFirst().span().start(),
        program.span().start(),
        "expected span to start at the first statement");
  }

  @Test
  void programSpanEndsAtEof() {
    assertEquals(program.eof().span().end(), program.span().end(), "expected span to end at eof");
  }

  @Test
  void parsesFirstStatementAsVariableDeclaration() {
    assertInstanceOf(
        VariableDeclarationSyntax.class,
        program.statements().get(0),
        "expected a variable declaration");
  }

  @Test
  void parsesThirdStatementAsAssignment() {
    assertInstanceOf(AssignmentSyntax.class, program.statements().get(2), "expected an assignment");
  }

  @Test
  void parsesAdditionAsTopLevelBinaryOperator() {
    assertEquals(TokenType.PLUS, plus.operator().type(), "expected top-level '+' operator");
  }

  @Test
  void parsesDivisionAsHigherPrecedenceThanAddition() {
    assertEquals(TokenType.SLASH, divide.operator().type(), "expected nested '/' operator");
  }

  @Test
  void parsesFourthStatementExpressionAsCall() {
    assertInstanceOf(
        CallExpressionSyntax.class, callStatement.expression(), "expected a call expression");
  }
}
