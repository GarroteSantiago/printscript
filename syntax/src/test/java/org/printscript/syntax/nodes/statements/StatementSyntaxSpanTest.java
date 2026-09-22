package org.printscript.syntax.nodes.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.printscript.lexer.KeywordTable;
import org.printscript.syntax.nodes.ProgramSyntax;
import org.printscript.testkit.TestSources;

class StatementSyntaxSpanTest {
  private static final String SOURCE =
      """
      let a: number = 1;
      a = a + 1;
      if (a) {
        println(a);
      } else {
        println(a);
      }
      println(a);
      """;

  private static final String SOURCE_IF_WITHOUT_ELSE =
      """
      let flag: boolean = true;
      if (flag) {
        println(flag);
      }
      """;

  private final ProgramSyntax program = TestSources.programOf(SOURCE, KeywordTable.v1_1());

  private IfStatementSyntax ifStatement() {
    return assertInstanceOf(
        IfStatementSyntax.class, program.statements().get(2), "expected an if statement");
  }

  private VariableDeclarationSyntax variableDeclaration() {
    return assertInstanceOf(
        VariableDeclarationSyntax.class, program.statements().getFirst(), "expected a declaration");
  }

  private AssignmentSyntax assignment() {
    return assertInstanceOf(
        AssignmentSyntax.class, program.statements().get(1), "expected an assignment");
  }

  private ExpressionStatementSyntax expressionStatement() {
    return assertInstanceOf(
        ExpressionStatementSyntax.class,
        program.statements().get(3),
        "expected an expression statement");
  }

  @Test
  void variableDeclarationSpanStartsAtKeyword() {
    var declaration = variableDeclaration();

    assertEquals(
        declaration.keyword().span().start(),
        declaration.span().start(),
        "expected span to start at the keyword");
  }

  @Test
  void variableDeclarationSpanEndsAtSemicolon() {
    var declaration = variableDeclaration();

    assertEquals(
        declaration.semicolon().span().end(),
        declaration.span().end(),
        "expected span to end at the semicolon");
  }

  @Test
  void assignmentSpanStartsAtIdentifier() {
    var assignment = assignment();

    assertEquals(
        assignment.identifier().span().start(),
        assignment.span().start(),
        "expected span to start at the identifier");
  }

  @Test
  void assignmentSpanEndsAtSemicolon() {
    var assignment = assignment();

    assertEquals(
        assignment.semicolon().span().end(),
        assignment.span().end(),
        "expected span to end at the semicolon");
  }

  @Test
  void assignmentAcceptDispatchesToVisitAssignment() {
    var assignment = assignment();

    assertEquals(
        "assignment",
        assignment.accept(NAMING_VISITOR),
        "expected accept() to dispatch to visitAssignment");
  }

  @Test
  void expressionStatementSpanStartsAtExpression() {
    var expressionStatement = expressionStatement();

    assertEquals(
        expressionStatement.expression().span().start(),
        expressionStatement.span().start(),
        "expected span to start at the expression");
  }

  @Test
  void expressionStatementSpanEndsAtSemicolon() {
    var expressionStatement = expressionStatement();

    assertEquals(
        expressionStatement.semicolon().span().end(),
        expressionStatement.span().end(),
        "expected span to end at the semicolon");
  }

  @Test
  void blockStatementSpanStartsAtLeftBrace() {
    var block = ifStatement().thenBlock();

    assertEquals(
        block.leftBrace().span().start(),
        block.span().start(),
        "expected span to start at the left brace");
  }

  @Test
  void blockStatementSpanEndsAtRightBrace() {
    var block = ifStatement().thenBlock();

    assertEquals(
        block.rightBrace().span().end(),
        block.span().end(),
        "expected span to end at the right brace");
  }

  @Test
  void blockStatementAcceptDispatchesToVisitBlock() {
    var block = ifStatement().thenBlock();

    assertEquals(
        "block", block.accept(NAMING_VISITOR), "expected accept() to dispatch to visitBlock");
  }

  @Test
  void ifStatementSpanStartsAtIfKeyword() {
    assertEquals(
        ifStatement().ifKeyword().span().start(),
        ifStatement().span().start(),
        "expected span to start at 'if'");
  }

  @Test
  void ifStatementWithElseSpanEndsAtElseBlock() {
    var ifStatement = ifStatement();

    assertEquals(
        ifStatement.elseBlock().orElseThrow().span().end(),
        ifStatement.span().end(),
        "expected span to end at the else block when present");
  }

  private IfStatementSyntax ifStatementWithoutElse() {
    var programWithoutElse = TestSources.programOf(SOURCE_IF_WITHOUT_ELSE, KeywordTable.v1_1());
    return assertInstanceOf(
        IfStatementSyntax.class,
        programWithoutElse.statements().get(1),
        "expected an if statement");
  }

  @Test
  void ifStatementWithoutElseSpanEndsAtThenBlock() {
    var ifStatement = ifStatementWithoutElse();

    assertEquals(
        ifStatement.thenBlock().span().end(),
        ifStatement.span().end(),
        "expected span to end at the then-block when there is no else");
  }

  @Test
  void ifStatementAcceptDispatchesToVisitIf() {
    assertEquals(
        "if", ifStatement().accept(NAMING_VISITOR), "expected accept() to dispatch to visitIf");
  }

  private static final StatementVisitor<String> NAMING_VISITOR =
      new StatementVisitor<>() {
        @Override
        public String visitVariableDeclaration(VariableDeclarationSyntax declaration) {
          return "variableDeclaration";
        }

        @Override
        public String visitAssignment(AssignmentSyntax assignment) {
          return "assignment";
        }

        @Override
        public String visitExpressionStatement(ExpressionStatementSyntax expressionStatement) {
          return "expressionStatement";
        }

        @Override
        public String visitIf(IfStatementSyntax ifStatement) {
          return "if";
        }

        @Override
        public String visitBlock(BlockStatementSyntax block) {
          return "block";
        }
      };
}
