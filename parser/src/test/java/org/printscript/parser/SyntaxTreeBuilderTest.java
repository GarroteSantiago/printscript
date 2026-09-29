package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.ProgramSyntax;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.ast.nodes.expressions.ExpressionVisitor;
import org.printscript.ast.nodes.expressions.IdentifierExpressionSyntax;
import org.printscript.ast.nodes.expressions.LiteralExpressionSyntax;
import org.printscript.ast.nodes.statements.AssignmentSyntax;
import org.printscript.ast.nodes.statements.BlockStatementSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.IfStatementSyntax;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.ast.nodes.statements.StatementVisitor;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.testkit.TestSources;
import org.printscript.tokens.SyntaxToken;

class SyntaxTreeBuilderTest {
  @Test
  public void preservesEverySourceCharacterAsTriviaOrTokenText() {
    String source =
        """
                # file comment
                let   name:string='Ada'; # inline comment

                println(name);\s\s
                """;

    StringBuilder rebuilt = new StringBuilder();
    for (SyntaxToken token : flatten(TestSources.programOf(source))) {
      rebuilt.append(token.leadingTrivia()).append(token.text());
    }

    assertEquals(
        source, rebuilt.toString(), "expected every source character preserved as trivia or text");
  }

  private List<SyntaxToken> flatten(ProgramSyntax program) {
    List<SyntaxToken> tokens = new ArrayList<>();
    for (StatementSyntax statement : program.statements()) addStatement(statement, tokens);
    tokens.add(program.eof());
    return tokens;
  }

  private void addStatement(StatementSyntax statement, List<SyntaxToken> tokens) {
    statement.accept(
        new StatementVisitor<Void>() {
          @Override
          public Void visitVariableDeclaration(VariableDeclarationSyntax declaration) {
            tokens.add(declaration.keyword());
            tokens.add(declaration.identifier());
            tokens.add(declaration.colon());
            tokens.add(declaration.type());
            if (declaration.equals().isPresent()) {
              tokens.add(declaration.equals().get());
              addExpression(declaration.initializer().orElseThrow(), tokens);
            }
            tokens.add(declaration.semicolon());
            return null;
          }

          @Override
          public Void visitAssignment(AssignmentSyntax assignment) {
            tokens.add(assignment.identifier());
            tokens.add(assignment.equals());
            addExpression(assignment.value(), tokens);
            tokens.add(assignment.semicolon());
            return null;
          }

          @Override
          public Void visitExpressionStatement(ExpressionStatementSyntax expressionStatement) {
            addExpression(expressionStatement.expression(), tokens);
            tokens.add(expressionStatement.semicolon());
            return null;
          }

          @Override
          public Void visitIf(IfStatementSyntax ifStatement) {
            tokens.add(ifStatement.ifKeyword());
            tokens.add(ifStatement.leftParen());
            addExpression(ifStatement.condition(), tokens);
            tokens.add(ifStatement.rightParen());
            addStatement(ifStatement.thenBlock(), tokens);
            if (ifStatement.elseKeyword().isPresent()) {
              tokens.add(ifStatement.elseKeyword().get());
              addStatement(ifStatement.elseBlock().orElseThrow(), tokens);
            }
            return null;
          }

          @Override
          public Void visitBlock(BlockStatementSyntax block) {
            tokens.add(block.leftBrace());
            for (StatementSyntax inner : block.statements()) {
              addStatement(inner, tokens);
            }
            tokens.add(block.rightBrace());
            return null;
          }
        });
  }

  private void addExpression(ExpressionSyntax expression, List<SyntaxToken> tokens) {
    expression.accept(
        new ExpressionVisitor<Void>() {
          @Override
          public Void visitLiteral(LiteralExpressionSyntax literal) {
            tokens.add(literal.literal());
            return null;
          }

          @Override
          public Void visitIdentifier(IdentifierExpressionSyntax identifier) {
            tokens.add(identifier.identifier());
            return null;
          }

          @Override
          public Void visitBinary(BinaryExpressionSyntax binary) {
            addExpression(binary.left(), tokens);
            tokens.add(binary.operator());
            addExpression(binary.right(), tokens);
            return null;
          }

          @Override
          public Void visitCall(CallExpressionSyntax call) {
            tokens.add(call.callee());
            tokens.add(call.leftParen());
            for (ExpressionSyntax argument : call.arguments()) {
              addExpression(argument, tokens);
            }
            tokens.add(call.rightParen());
            return null;
          }
        });
  }
}
