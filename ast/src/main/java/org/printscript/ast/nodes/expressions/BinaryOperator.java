package org.printscript.ast.nodes.expressions;

/**
 * The operator of a {@link BinaryExpressionSyntax}: language meaning, not the lexer token it was
 * spelled with. The parser translates the token into one of these singletons, so the stages after
 * it (typechecker, interpreter) dispatch on the operator through {@link BinaryOperatorVisitor}
 * without depending on {@code tokens}.
 */
public interface BinaryOperator {
  <R> R accept(BinaryOperatorVisitor<R> visitor);

  /** How the operator is written in source, e.g. {@code +}; used in diagnostics. */
  String symbol();

  BinaryOperator ADD =
      new Singleton("+", "ADD") {
        @Override
        public <R> R accept(BinaryOperatorVisitor<R> visitor) {
          return visitor.visitAdd();
        }
      };

  BinaryOperator SUBTRACT =
      new Singleton("-", "SUBTRACT") {
        @Override
        public <R> R accept(BinaryOperatorVisitor<R> visitor) {
          return visitor.visitSubtract();
        }
      };

  BinaryOperator MULTIPLY =
      new Singleton("*", "MULTIPLY") {
        @Override
        public <R> R accept(BinaryOperatorVisitor<R> visitor) {
          return visitor.visitMultiply();
        }
      };

  BinaryOperator DIVIDE =
      new Singleton("/", "DIVIDE") {
        @Override
        public <R> R accept(BinaryOperatorVisitor<R> visitor) {
          return visitor.visitDivide();
        }
      };

  /** Shared {@link #symbol()}/{@link #toString()} for the singletons above. */
  abstract class Singleton implements BinaryOperator {
    private final String symbol;
    private final String name;

    private Singleton(String symbol, String name) {
      this.symbol = symbol;
      this.name = name;
    }

    @Override
    public String symbol() {
      return symbol;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
