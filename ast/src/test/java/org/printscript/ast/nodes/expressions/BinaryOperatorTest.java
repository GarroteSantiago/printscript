package org.printscript.ast.nodes.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class BinaryOperatorTest {
  private static final List<BinaryOperator> ALL =
      List.of(
          BinaryOperator.ADD,
          BinaryOperator.SUBTRACT,
          BinaryOperator.MULTIPLY,
          BinaryOperator.DIVIDE);

  private static final BinaryOperatorVisitor<String> NAMING_VISITOR =
      new BinaryOperatorVisitor<>() {
        @Override
        public String visitAdd() {
          return "add";
        }

        @Override
        public String visitSubtract() {
          return "subtract";
        }

        @Override
        public String visitMultiply() {
          return "multiply";
        }

        @Override
        public String visitDivide() {
          return "divide";
        }
      };

  @Test
  public void eachOperatorDispatchesToItsOwnVisitMethod() {
    assertEquals(
        List.of("add", "subtract", "multiply", "divide"),
        ALL.stream().map(operator -> operator.accept(NAMING_VISITOR)).toList(),
        "expected accept() to dispatch to the matching visit method");
  }

  @Test
  public void eachOperatorReportsItsSourceSymbol() {
    assertEquals(
        List.of("+", "-", "*", "/"),
        ALL.stream().map(BinaryOperator::symbol).toList(),
        "expected each operator's symbol as written in source");
  }

  @Test
  public void eachOperatorHasAReadableName() {
    assertEquals(
        List.of("ADD", "SUBTRACT", "MULTIPLY", "DIVIDE"),
        ALL.stream().map(Object::toString).toList(),
        "expected toString() to name the operator");
  }
}
