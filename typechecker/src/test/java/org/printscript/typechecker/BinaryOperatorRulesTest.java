package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.tokens.TokenType;
import org.printscript.types.TypeName;

class BinaryOperatorRulesTest {
  private final BinaryOperatorRules rules = BinaryOperatorRules.v1();

  @Test
  public void plusConcatenatesWhenTheLeftOperandIsString() {
    assertEquals(
        Optional.of(TypeName.STRING),
        rules.resultType(TokenType.PLUS, TypeName.STRING, TypeName.NUMBER),
        "expected string concatenation when the left operand is a string");
  }

  @Test
  public void plusConcatenatesWhenTheRightOperandIsString() {
    assertEquals(
        Optional.of(TypeName.STRING),
        rules.resultType(TokenType.PLUS, TypeName.NUMBER, TypeName.STRING),
        "expected string concatenation when the right operand is a string");
  }

  @Test
  public void arithmeticOperatorsResultInNumberForTwoNumbers() {
    assertEquals(
        Optional.of(TypeName.NUMBER),
        rules.resultType(TokenType.MINUS, TypeName.NUMBER, TypeName.NUMBER),
        "expected a numeric result for two numbers");
  }

  @Test
  public void plusResultsInNumberForTwoNumbers() {
    assertEquals(
        Optional.of(TypeName.NUMBER),
        rules.resultType(TokenType.PLUS, TypeName.NUMBER, TypeName.NUMBER),
        "expected a numeric result for plus on two numbers");
  }

  @Test
  public void rejectsOperandsThatAreNeitherStringNorBothNumbers() {
    assertTrue(
        rules.resultType(TokenType.PLUS, TypeName.BOOLEAN, TypeName.BOOLEAN).isEmpty(),
        "expected no result type for two booleans");
  }

  @Test
  public void rejectsNonPlusOperatorsOnStrings() {
    assertTrue(
        rules.resultType(TokenType.MINUS, TypeName.STRING, TypeName.STRING).isEmpty(),
        "expected subtraction on strings to be rejected");
  }
}
