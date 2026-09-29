package org.printscript.interpreter;

import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.typetable.SemanticModel;

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
