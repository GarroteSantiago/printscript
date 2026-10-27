package org.printscript.interpreter;

import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;
import org.printscript.typechecker.SemanticContext;
import org.printscript.typechecker.ValidatingStatementSource;
import org.printscript.typetable.ValidatedStatementSource;

/**
 * Test helper: validates and executes a source one statement at a time through {@link
 * Interpreter#executeAll}, exactly as {@code toolchain} does.
 */
final class Execution {
  private Execution() {}

  static ValidatedStatementSource validated(String source) {
    return new ValidatingStatementSource(
        TestSources.statementsOf(source), SemanticContext.forVersion(false));
  }

  static ValidatedStatementSource validatedV11(String source) {
    return new ValidatingStatementSource(
        TestSources.statementsOf(source, KeywordTable.v1_1()), SemanticContext.forVersion(true));
  }

  static RuntimeEnvironment run(Interpreter interpreter, ValidatedStatementSource statements) {
    return interpreter.executeAll(statements, RuntimeEnvironment.empty());
  }
}
