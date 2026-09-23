package org.printscript.interpreter;

import java.util.Map;
import java.util.Optional;
import org.printscript.typetable.BuiltinRegistry;

/**
 * Runtime behavior for each builtin, keyed by the same name {@link BuiltinRegistry} resolves
 * signatures for. Adding a builtin's execution is one entry here; whether it's actually callable
 * for a given language version is still gated by {@code BuiltinRegistry} at the semantic phase.
 */
final class BuiltinBehaviors {
  private static final Map<String, BuiltinBehavior> BEHAVIORS =
      Map.of(
          BuiltinRegistry.PRINTLN, BuiltinBehaviors::println,
          BuiltinRegistry.READ_INPUT, BuiltinBehaviors::readInput,
          BuiltinRegistry.READ_ENV, BuiltinBehaviors::readEnv);

  private BuiltinBehaviors() {}

  static Optional<BuiltinBehavior> find(String name) {
    return Optional.ofNullable(BEHAVIORS.get(name));
  }

  private static RuntimeValue println(RuntimeValue argument, BuiltinRuntimeContext context) {
    context.output().println(context.stringify(argument));
    return RuntimeValue.UnitValue.INSTANCE;
  }

  private static RuntimeValue readInput(RuntimeValue argument, BuiltinRuntimeContext context) {
    String raw;
    try {
      raw = context.input().readLine(context.stringify(argument));
    } catch (UnsupportedOperationException unsupported) {
      RuntimeFailure failure = context.runtimeFailure(unsupported.getMessage());
      failure.initCause(unsupported);
      throw failure;
    }
    return context.parseValue(raw);
  }

  private static RuntimeValue readEnv(RuntimeValue argument, BuiltinRuntimeContext context) {
    String variableName = context.stringify(argument);
    String raw;
    try {
      raw =
          context
              .env()
              .get(variableName)
              .orElseThrow(
                  () ->
                      context.runtimeFailure(
                          "Environment variable '" + variableName + "' is not set"));
    } catch (UnsupportedOperationException unsupported) {
      RuntimeFailure failure = context.runtimeFailure(unsupported.getMessage());
      failure.initCause(unsupported);
      throw failure;
    }
    return context.parseValue(raw);
  }
}
