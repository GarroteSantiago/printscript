package org.printscript.semantics;

import java.util.Map;
import java.util.Optional;
import org.printscript.syntax.TypeName;

public final class BuiltinRegistry {
  public static final String PRINTLN = "println";
  public static final String READ_INPUT = "readInput";
  public static final String READ_ENV = "readEnv";
  private final Map<String, BuiltinSignature> signatures;

  private BuiltinRegistry(Map<String, BuiltinSignature> signatures) {
    this.signatures = Map.copyOf(signatures);
  }

  public static BuiltinRegistry v1() {
    return new BuiltinRegistry(
        Map.of(PRINTLN, BuiltinSignature.printing(PRINTLN, TypeName.STRING)));
  }

  public static BuiltinRegistry v1_1() {
    return new BuiltinRegistry(
        Map.of(
            PRINTLN,
            BuiltinSignature.printing(PRINTLN, TypeName.STRING),
            READ_INPUT,
            BuiltinSignature.contextual(READ_INPUT, java.util.List.of(TypeName.STRING)),
            READ_ENV,
            BuiltinSignature.contextual(READ_ENV, java.util.List.of(TypeName.STRING))));
  }

  public Optional<BuiltinSignature> find(String name) {
    return Optional.ofNullable(signatures.get(name));
  }
}
