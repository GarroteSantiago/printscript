/**
 * The sole production parser: {@link org.printscript.parser.StatementSyntaxReader} implements
 * {@link org.printscript.syntax.StatementSource} by pulling from a {@code
 * org.printscript.tokens.TokenSource} (never a concrete {@code Lexer} directly). {@link
 * org.printscript.parser.SyntaxTreeBuilder} drains a {@code StatementSource} into a fully
 * materialized tree, for callers that want the whole program at once rather than streaming
 * statement-by-statement.
 *
 * <p>Split out from {@code syntax} on purpose: {@code formatter}, {@code interpreter}, and {@code
 * analyzer} all walk the AST {@code syntax} defines, but none of them parse — they receive an
 * already-built tree from {@code application}. Depending on this module at all is a signal that a
 * consumer builds trees, not just walks them.
 */
package org.printscript.parser;
