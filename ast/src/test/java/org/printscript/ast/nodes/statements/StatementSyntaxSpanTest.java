package org.printscript.ast.nodes.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.lexer.KeywordTable;
import org.printscript.source.SourcePosition;
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

  private final List<StatementSyntax> parsed =
      TestSources.statementListOf(SOURCE, KeywordTable.v1_1());

  private static String at(SourcePosition position) {
    return position.row() + ":" + position.column();
  }

  private IfStatementSyntax ifStatement() {
    return assertInstanceOf(IfStatementSyntax.class, parsed.get(2), "expected an if statement");
  }

  private VariableDeclarationSyntax variableDeclaration() {
    return assertInstanceOf(
        VariableDeclarationSyntax.class, parsed.getFirst(), "expected a declaration");
  }

  private AssignmentSyntax assignment() {
    return assertInstanceOf(AssignmentSyntax.class, parsed.get(1), "expected an assignment");
  }

  private ExpressionStatementSyntax expressionStatement() {
    return assertInstanceOf(
        ExpressionStatementSyntax.class, parsed.get(3), "expected an expression statement");
  }

  @Test
  public void variableDeclarationSpanStartsAtKeyword() {
    var declaration = variableDeclaration();

    assertEquals("1:1", at(declaration.span().start()), "expected span to start at the keyword");
  }

  @Test
  public void variableDeclarationSpanEndsAtSemicolon() {
    var declaration = variableDeclaration();

    assertEquals("1:18", at(declaration.span().end()), "expected span to end at the semicolon");
  }

  @Test
  public void assignmentSpanStartsAtIdentifier() {
    var assignment = assignment();

    assertEquals("2:1", at(assignment.span().start()), "expected span to start at the identifier");
  }

  @Test
  public void assignmentSpanEndsAtSemicolon() {
    var assignment = assignment();

    assertEquals("2:10", at(assignment.span().end()), "expected span to end at the semicolon");
  }

  @Test
  public void assignmentAcceptDispatchesToVisitAssignment() {
    var assignment = assignment();

    assertEquals(
        "assignment",
        assignment.accept(NAMING_VISITOR),
        "expected accept() to dispatch to visitAssignment");
  }

  @Test
  public void expressionStatementSpanStartsAtExpression() {
    var expressionStatement = expressionStatement();

    assertEquals(
        expressionStatement.expression().span().start(),
        expressionStatement.span().start(),
        "expected span to start at the expression");
  }

  @Test
  public void expressionStatementSpanEndsAtSemicolon() {
    var expressionStatement = expressionStatement();

    assertEquals(
        "8:11", at(expressionStatement.span().end()), "expected span to end at the semicolon");
  }

  @Test
  public void blockStatementSpanStartsAtLeftBrace() {
    var block = ifStatement().thenBlock();

    assertEquals("3:8", at(block.span().start()), "expected span to start at the left brace");
  }

  @Test
  public void blockStatementSpanEndsAtRightBrace() {
    var block = ifStatement().thenBlock();

    assertEquals("5:1", at(block.span().end()), "expected span to end at the right brace");
  }

  @Test
  public void blockStatementAcceptDispatchesToVisitBlock() {
    var block = ifStatement().thenBlock();

    assertEquals(
        "block", block.accept(NAMING_VISITOR), "expected accept() to dispatch to visitBlock");
  }

  @Test
  public void ifStatementSpanStartsAtIfKeyword() {
    assertEquals("3:1", at(ifStatement().span().start()), "expected span to start at 'if'");
  }

  @Test
  public void ifStatementWithElseSpanEndsAtElseBlock() {
    var ifStatement = ifStatement();

    assertEquals(
        ifStatement.elseBlock().orElseThrow().span().end(),
        ifStatement.span().end(),
        "expected span to end at the else block when present");
  }

  private IfStatementSyntax ifStatementWithoutElse() {
    var parsedWithoutElse =
        TestSources.statementListOf(SOURCE_IF_WITHOUT_ELSE, KeywordTable.v1_1());
    return assertInstanceOf(
        IfStatementSyntax.class, parsedWithoutElse.get(1), "expected an if statement");
  }

  @Test
  public void ifStatementWithoutElseSpanEndsAtThenBlock() {
    var ifStatement = ifStatementWithoutElse();

    assertEquals(
        ifStatement.thenBlock().span().end(),
        ifStatement.span().end(),
        "expected span to end at the then-block when there is no else");
  }

  @Test
  public void ifStatementAcceptDispatchesToVisitIf() {
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
