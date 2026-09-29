package org.printscript.repl;

import java.util.List;
import java.util.function.Consumer;
import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.interpreter.ArithmeticOperators;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.RuntimeEnvironment;
import org.printscript.interpreter.RuntimeFailure;
import org.printscript.tokens.SyntaxException;
import org.printscript.typechecker.SemanticContext;
import org.printscript.typechecker.SemanticStatementResult;
import org.printscript.typetable.ValidatedStatement;

/**
 * Owns the state a REPL needs across statements — a {@link SemanticContext} and a {@link
 * RuntimeEnvironment} — so {@code toolchain} and the language core stay exactly as stateless as
 * they are for batch execution. {@link SemanticContext#validate} already returns the *next*
 * immutable context instead of mutating {@code this}, and {@link Interpreter#executeAll} already
 * threads a {@link RuntimeEnvironment} as a value; this class is nothing more than two local
 * variables holding onto those values between calls, the same role a REPL driver plays in any
 * language with an immutable core (e.g. GHCi holding the current typing context between commands).
 *
 * <p>{@link #step} evaluates exactly one statement pulled from a caller-supplied {@link
 * StatementSource} and never throws: a syntax error, a semantic error, or a runtime failure are all
 * reported as diagnostics, and the session's state is left exactly as it was before the failing
 * statement — the same "a failed statement never contributes symbols" rule {@code SemanticContext}
 * already documents for batch validation.
 *
 * <p>One known rough edge: after a syntax error, the underlying {@code StatementSyntaxReader}'s
 * one-token lookahead may not be sitting cleanly at the next statement's first token, so a badly
 * malformed line can cascade into a second, spurious diagnostic before the session recovers. The
 * language core deliberately has no parser recovery for batch compilation; a REPL that resyncs
 * perfectly after any malformed input is future work, not required for a first working REPL.
 */
public final class ReplSession {
  private final Interpreter interpreter;
  private SemanticContext context;
  private RuntimeEnvironment environment;

  /**
   * Starts a fresh session for {@code v11} (v1.1 vs v1.0), writing println output to {@code
   * output}.
   */
  public ReplSession(boolean v11, Consumer<String> output) {
    this.context = SemanticContext.forVersion(v11);
    this.interpreter = new Interpreter(output::accept, ArithmeticOperators.v1());
    this.environment = RuntimeEnvironment.empty();
  }

  /**
   * Parses, validates, and executes one statement pulled from {@code statements}. Caller must have
   * already checked {@code statements.hasNext()}. Returns an empty list on success; a non-empty
   * list of diagnostics on any failure, with the session's state unchanged.
   */
  public List<Diagnostic> step(StatementSource statements) {
    StatementSyntax statement;
    try {
      statement = statements.next();
    } catch (SyntaxException exception) {
      return List.of(exception.diagnostic());
    }

    SemanticStatementResult validated = context.validate(statement);
    if (!validated.isSuccess()) {
      return validated.diagnostics();
    }

    ValidatedStatement validatedStatement =
        new ValidatedStatement(statement, validated.semanticModel());
    try {
      environment =
          interpreter.executeAll(
              new SingleValidatedStatementSource(validatedStatement), environment);
    } catch (RuntimeFailure failure) {
      return List.of(failure.diagnostic());
    }

    context = validated.nextContext();
    return List.of();
  }
}
