/**
 * The sole production parser: {@link org.printscript.parser.StatementSyntaxReader} implements
 * {@link org.printscript.ast.StatementSource} by pulling from a {@code
 * org.printscript.tokens.TokenSource} (never a concrete {@code Lexer} directly), one statement at a
 * time.
 *
 * <p>Split out from {@code ast} on purpose: {@code interpreter} and {@code analyzer} walk the AST
 * {@code ast} defines, but neither parses — they receive already-parsed statements from {@code
 * toolchain}. Depending on this module at all is a signal that a consumer builds trees, not just
 * walks them.
 */
package org.printscript.parser;
