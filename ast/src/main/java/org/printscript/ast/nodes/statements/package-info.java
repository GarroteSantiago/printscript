/**
 * Statement AST nodes: {@link org.printscript.ast.nodes.statements.VariableDeclarationSyntax},
 * {@link org.printscript.ast.nodes.statements.AssignmentSyntax}, {@link
 * org.printscript.ast.nodes.statements.ExpressionStatementSyntax}, {@link
 * org.printscript.ast.nodes.statements.IfStatementSyntax}, and {@link
 * org.printscript.ast.nodes.statements.BlockStatementSyntax}. Every consumer dispatches on these
 * through {@link org.printscript.ast.nodes.statements.StatementVisitor}, never {@code instanceof} —
 * start there to see every place a new statement kind would need a case.
 */
package org.printscript.ast.nodes.statements;
