package org.printscript.typetable;

import java.util.List;
import org.printscript.types.TypeName;

public record BuiltinSignature(
    String name,
    List<TypeName> parameterTypes,
    TypeName returnType,
    boolean contextual,
    boolean printsAnyType) {
  public BuiltinSignature {
    parameterTypes = List.copyOf(parameterTypes);
  }

  public BuiltinSignature(String name, List<TypeName> parameterTypes, TypeName returnType) {
    this(name, parameterTypes, returnType, false, false);
  }

  public static BuiltinSignature contextual(String name, List<TypeName> parameterTypes) {
    return new BuiltinSignature(name, parameterTypes, null, true, false);
  }

  /**
   * A builtin whose sole parameter accepts any printable type (string/number/boolean), even though
   * {@code canonicalParameterType} is what nested contextual calls (readInput/readEnv) resolve to
   * when used as this builtin's argument.
   */
  public static BuiltinSignature printing(String name, TypeName canonicalParameterType) {
    return new BuiltinSignature(name, List.of(canonicalParameterType), null, false, true);
  }
}
