package org.printscript.interpreter;

import java.math.BigDecimal;
import java.math.MathContext;
import org.printscript.ast.nodes.expressions.BinaryOperator;
import org.printscript.ast.nodes.expressions.BinaryOperatorVisitor;

/**
 * What a binary operator ({@code +}/{@code -}/{@code *}/{@code /}) computes given two numeric
 * operands — the swappable, version-specific counterpart to {@code
 * typechecker.BinaryOperatorRules}, which already decided *that* the operator applies to these
 * operand types before this interface is asked to compute the result. {@link #v1()} uses {@link
 * java.math.MathContext#DECIMAL128} decimal division and throws {@link ArithmeticException} on
 * division by zero, which {@code Interpreter} converts to a runtime {@code DiagnosticException}.
 */
@FunctionalInterface
public interface ArithmeticOperators {
  BigDecimal apply(BinaryOperator operator, BigDecimal left, BigDecimal right);

  static ArithmeticOperators v1() {
    return (operator, left, right) -> operator.accept(new V1Visitor(left, right));
  }

  final class V1Visitor implements BinaryOperatorVisitor<BigDecimal> {
    private final BigDecimal left;
    private final BigDecimal right;

    private V1Visitor(BigDecimal left, BigDecimal right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public BigDecimal visitAdd() {
      return left.add(right);
    }

    @Override
    public BigDecimal visitSubtract() {
      return left.subtract(right);
    }

    @Override
    public BigDecimal visitMultiply() {
      return left.multiply(right);
    }

    @Override
    public BigDecimal visitDivide() {
      if (right.compareTo(BigDecimal.ZERO) == 0) {
        throw new ArithmeticException("Division by zero");
      }
      return left.divide(right, MathContext.DECIMAL128).stripTrailingZeros();
    }
  }
}
