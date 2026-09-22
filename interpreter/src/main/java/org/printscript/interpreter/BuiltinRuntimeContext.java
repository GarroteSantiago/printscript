package org.printscript.interpreter;

import org.printscript.semantics.SemanticModel;
import org.printscript.syntax.nodes.expressions.CallExpressionSyntax;

/** What a {@link BuiltinBehavior} needs from the interpreter to do its work. */
public interface BuiltinRuntimeContext {
  CallExpressionSyntax call();

  SemanticModel semanticModel();

  OutputPort output();

  InputPort input();

  EnvironmentPort env();

  String stringify(RuntimeValue value);

  RuntimeValue parseValue(String raw);

  RuntimeFailure runtimeFailure(String message);
}
