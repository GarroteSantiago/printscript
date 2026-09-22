package org.printscript.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable variable bindings. {@link #put} returns a new {@code RuntimeEnvironment} rather than
 * mutating this one — the interpreter is always replacing its state object, never mutating it in
 * place, which is what lets {@link Interpreter#executeStatement} be called safely across statements
 * from a streaming {@code StatementSource}.
 */
public record RuntimeEnvironment(Map<String, RuntimeValue> values) {
  public RuntimeEnvironment {
    values = Map.copyOf(values);
  }

  public static RuntimeEnvironment empty() {
    return new RuntimeEnvironment(Map.of());
  }

  public Optional<RuntimeValue> find(String name) {
    return Optional.ofNullable(values.get(name));
  }

  public RuntimeEnvironment put(String name, RuntimeValue value) {
    Map<String, RuntimeValue> next = new HashMap<>(values);
    next.put(name, value);
    return new RuntimeEnvironment(next);
  }
}
