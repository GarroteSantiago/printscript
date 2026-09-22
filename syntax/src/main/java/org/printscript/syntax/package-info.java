/**
 * The AST contract: the {@link org.printscript.syntax.StatementSource} port a parser implements,
 * and the swappable, version-specific tables consulted while walking a tree ({@link
 * org.printscript.syntax.TypeAnnotationTable}). See {@link org.printscript.syntax.nodes} for the
 * AST shape itself, and the module's {@code ARCHITECTURE.md} for how this fits between {@code
 * tokens} and {@code semantics}.
 *
 * <p>This module does not parse anything itself — {@code org.printscript.parser} is the sole
 * production implementation of {@link org.printscript.syntax.StatementSource}. Consumers that only
 * walk an already-built tree (formatter, interpreter, analyzer) depend on this module alone, never
 * on the parser that built it.
 */
package org.printscript.syntax;
