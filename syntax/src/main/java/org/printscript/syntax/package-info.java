/**
 * The parser and the swappable, version-specific tables it consults ({@link
 * org.printscript.syntax.TypeAnnotationTable}). See {@link org.printscript.syntax.nodes} for the
 * AST shape this package produces, and the module's {@code ARCHITECTURE.md} for how this fits
 * between {@code tokens} and {@code semantics}.
 *
 * <p>Start at {@link org.printscript.syntax.StatementSource} (the port) and {@link
 * org.printscript.syntax.StatementSyntaxReader} (the parser that implements it).
 */
package org.printscript.syntax;
