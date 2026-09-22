package org.printscript.semantics;

import java.util.Optional;
import org.printscript.syntax.TypeName;
import org.printscript.tokens.TokenType;

@FunctionalInterface
public interface BinaryOperatorRules {
  Optional<TypeName> resultType(TokenType operator, TypeName left, TypeName right);

  static BinaryOperatorRules v1() {
    return (operator, left, right) -> {
      if (TokenType.PLUS.equals(operator)
          && (TypeName.STRING.equals(left) || TypeName.STRING.equals(right))) {
        return Optional.of(TypeName.STRING);
      }
      if (TypeName.NUMBER.equals(left) && TypeName.NUMBER.equals(right)) {
        return Optional.of(TypeName.NUMBER);
      }
      return Optional.empty();
    };
  }
}
