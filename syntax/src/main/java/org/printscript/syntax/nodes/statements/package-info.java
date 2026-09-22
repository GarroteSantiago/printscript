/**
 * Statement AST nodes: {@link org.printscript.syntax.nodes.statements.VariableDeclarationSyntax},
 * {@link org.printscript.syntax.nodes.statements.AssignmentSyntax}, {@link
 * org.printscript.syntax.nodes.statements.ExpressionStatementSyntax}, {@link
 * org.printscript.syntax.nodes.statements.IfStatementSyntax}, and {@link
 * org.printscript.syntax.nodes.statements.BlockStatementSyntax}. Every consumer dispatches on these
 * through {@link org.printscript.syntax.nodes.statements.StatementVisitor}, never {@code
 * instanceof} — start there to see every place a new statement kind would need a case.
 */
package org.printscript.syntax.nodes.statements;
