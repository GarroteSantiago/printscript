package org.printscript.typetable;

/**
 * Pull-based port for a stream of already-validated statements, one layer above {@code
 * ast.StatementSource}. {@code hasNext()}/{@code next()} let a consumer (an interpreter, analyzer,
 * or the {@code application} composition root) process one validated statement at a time; {@code
 * next()} throws {@link SemanticException} the moment a statement fails validation, mirroring how
 * {@code ast.StatementSource}/{@code tokens.TokenSource} signal a syntax error, rather than
 * returning a result the caller must remember to check. {@code
 * typechecker.ValidatingStatementSource} is the sole production implementation.
 */
public interface ValidatedStatementSource {
  boolean hasNext();

  ValidatedStatement next();

  /** Exhausts the source without doing anything with the statements — the whole of "validate". */
  default void drain() {
    while (hasNext()) {
      next();
    }
  }
}
