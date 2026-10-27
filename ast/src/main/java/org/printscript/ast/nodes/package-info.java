/**
 * The AST: {@link org.printscript.ast.nodes.SyntaxNode} at the root. Concrete expression and
 * statement node kinds live one level down, in {@link org.printscript.ast.nodes.expressions} and
 * {@link org.printscript.ast.nodes.statements}. There is no whole-program node: programs are
 * processed one statement at a time, through {@link org.printscript.ast.StatementSource}.
 */
package org.printscript.ast.nodes;
