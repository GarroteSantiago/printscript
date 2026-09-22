package org.printscript.interpreter;

import java.math.BigDecimal;
import java.math.MathContext;
import org.printscript.tokens.TokenType;
import org.printscript.tokens.TokenTypeVisitor;

/**
 * What a binary operator ({@code +}/{@code -}/{@code *}/{@code /}) computes given two numeric
 * operands — the swappable, version-specific counterpart to {@link
 * org.printscript.semantics.BinaryOperatorRules}, which already decided *that* the operator applies
 * to these operand types before this interface is asked to compute the result. {@link #v1()} uses
 * {@link java.math.MathContext#DECIMAL128} decimal division and throws {@link ArithmeticException}
 * on division by zero, which {@code Interpreter} converts to a {@link RuntimeFailure}.
 */
@FunctionalInterface
public interface ArithmeticOperators {
  BigDecimal apply(TokenType operator, BigDecimal left, BigDecimal right);

  static ArithmeticOperators v1() {
    return (operator, left, right) -> operator.accept(new V1Visitor(left, right));
  }

  final class V1Visitor implements TokenTypeVisitor<BigDecimal> {
    private final BigDecimal left;
    private final BigDecimal right;

    V1Visitor(BigDecimal left, BigDecimal right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public BigDecimal visitPlus() {
      return left.add(right);
    }

    @Override
    public BigDecimal visitMinus() {
      return left.subtract(right);
    }

    @Override
    public BigDecimal visitStar() {
      return left.multiply(right);
    }

    @Override
    public BigDecimal visitSlash() {
      if (right.compareTo(BigDecimal.ZERO) == 0) {
        throw new ArithmeticException("Division by zero");
      }
      return left.divide(right, MathContext.DECIMAL128).stripTrailingZeros();
    }

    @Override
    public BigDecimal visitLet() {
      return unsupported("LET");
    }

    @Override
    public BigDecimal visitIdentifier() {
      return unsupported("IDENTIFIER");
    }

    @Override
    public BigDecimal visitType() {
      return unsupported("TYPE");
    }

    @Override
    public BigDecimal visitNumber() {
      return unsupported("NUMBER");
    }

    @Override
    public BigDecimal visitString() {
      return unsupported("STRING");
    }

    @Override
    public BigDecimal visitColon() {
      return unsupported("COLON");
    }

    @Override
    public BigDecimal visitSemicolon() {
      return unsupported("SEMICOLON");
    }

    @Override
    public BigDecimal visitEqual() {
      return unsupported("EQUAL");
    }

    @Override
    public BigDecimal visitLeftParen() {
      return unsupported("LEFT_PAREN");
    }

    @Override
    public BigDecimal visitRightParen() {
      return unsupported("RIGHT_PAREN");
    }

    @Override
    public BigDecimal visitLeftBrace() {
      return unsupported("LEFT_BRACE");
    }

    @Override
    public BigDecimal visitRightBrace() {
      return unsupported("RIGHT_BRACE");
    }

    @Override
    public BigDecimal visitConst() {
      return unsupported("CONST");
    }

    @Override
    public BigDecimal visitIf() {
      return unsupported("IF");
    }

    @Override
    public BigDecimal visitElse() {
      return unsupported("ELSE");
    }

    @Override
    public BigDecimal visitBoolean() {
      return unsupported("BOOLEAN");
    }

    @Override
    public BigDecimal visitComment() {
      return unsupported("COMMENT");
    }

    @Override
    public BigDecimal visitEof() {
      return unsupported("EOF");
    }

    private BigDecimal unsupported(String tokenName) {
      throw new IllegalStateException("Unsupported binary operator: " + tokenName);
    }
  }
}
