package org.printscript.ast.nodes.expressions;

/**
 * One method per {@link BinaryOperator}. Adding an operator adds a method here, which fails
 * compilation in every visitor until it handles the new operator.
 */
public interface BinaryOperatorVisitor<R> {
  R visitAdd();

  R visitSubtract();

  R visitMultiply();

  R visitDivide();
}
