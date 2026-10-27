package org.printscript.ast;

import org.printscript.ast.nodes.statements.StatementSyntax;

/**
 * Pull-based port for a stream of parsed statements, analogous to {@code tokens.TokenSource} one
 * layer up. {@code hasNext()}/{@code next()} let a consumer (the {@code toolchain} composition
 * root, or a test) process one statement at a time without materializing a full {@link
 * org.printscript.ast.nodes.ProgramSyntax}. {@code parser.StatementSyntaxReader} is the sole
 * production implementation.
 */
public interface StatementSource {
  boolean hasNext();

  StatementSyntax next();
}
