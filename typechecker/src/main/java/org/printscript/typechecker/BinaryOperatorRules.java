package org.printscript.typechecker;

import java.util.Optional;
import org.printscript.tokens.TokenType;
import org.printscript.types.TypeName;

/**
 * The single source of truth for what result type, if any, a binary operator produces given its
 * operand types — an absent result means the combination is a type error. {@code
 * interpreter.Interpreter} deliberately does not reimplement this decision at runtime; it only acts
 * on the {@link TypeName} this rule already assigned during validation (via {@link
 * org.printscript.semantics.SemanticModel#typeOf}). If a change is tempted at the interpreter level
 * to decide operator behavior from runtime values, it almost certainly belongs here instead.
 */
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
