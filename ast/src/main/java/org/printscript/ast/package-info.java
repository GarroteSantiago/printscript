/**
 * The AST contract: the {@link org.printscript.ast.StatementSource} port a parser implements. See
 * {@link org.printscript.ast.nodes} for the AST shape itself, {@code org.printscript.types} for the
 * type-system vocabulary a node like {@code LiteralExpressionSyntax} references, and the module's
 * {@code ARCHITECTURE.md} for how this fits between {@code tokens} and {@code typetable}.
 *
 * <p>This module does not parse anything itself — {@code org.printscript.parser} is the sole
 * production implementation of {@link org.printscript.ast.StatementSource}. Consumers that only
 * walk an already-built tree (formatter, interpreter, analyzer) depend on this module alone, never
 * on the parser that built it.
 */
package org.printscript.ast;
