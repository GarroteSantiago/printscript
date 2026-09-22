package org.printscript.syntax.nodes.statements;

public interface StatementVisitor<R> {
  R visitVariableDeclaration(VariableDeclarationSyntax declaration);

  R visitAssignment(AssignmentSyntax assignment);

  R visitExpressionStatement(ExpressionStatementSyntax expressionStatement);

  R visitIf(IfStatementSyntax ifStatement);

  R visitBlock(BlockStatementSyntax block);
}
