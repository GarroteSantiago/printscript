package org.printscript.repl;

import java.util.List;
import java.util.function.Consumer;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.interpreter.ArithmeticOperators;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.RuntimeEnvironment;
import org.printscript.interpreter.RuntimeFailure;
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
 * <p>{@link #step} validates and executes one already-parsed {@code statement} and never throws: a
 * semantic error or a runtime failure is reported as diagnostics, and the session's state is left
 * exactly as it was before the failing statement — the same "a failed statement never contributes
 * symbols" rule {@code SemanticContext} already documents for batch validation.
 *
 * <p>Parsing is deliberately not this class's job: a syntax error can leave the {@code
 * StatementSyntaxReader}/{@code Lexer} pair that produced the statement in a cursor state that
 * cannot safely be pulled from again (its one-token lookahead can get stuck re-failing on the same
 * token forever). Recovering from that means discarding and rebuilding that pair over the same
 * underlying {@code Reader} — a decision only {@link Repl}, which owns their construction, can
 * make. This class only ever receives a {@link StatementSyntax} that already parsed successfully.
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
   * Validates and executes one already-parsed {@code statement}. Returns an empty list on success;
   * a non-empty list of diagnostics on a semantic or runtime failure, with the session's state
   * unchanged.
   */
  public List<Diagnostic> step(StatementSyntax statement) {
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
