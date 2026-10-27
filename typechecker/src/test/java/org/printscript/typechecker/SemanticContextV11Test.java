package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemanticContextV11Test {
  private static final String EXPECTED_ONE_DIAGNOSTIC = "expected one diagnostic";
  private static final String EXPECTED_NO_DIAGNOSTICS = "expected no diagnostics";

  private static final String CONST_REASSIGNMENT =
      """
      const x: number = 1;
      x = 2;
      """;

  private static final String LET_REASSIGNMENT =
      """
      let x: number = 1;
      x = 2;
      """;

  private static final String NON_BOOLEAN_IF_CONDITION =
      """
      let x: number = 1;
      if (x) {
        println("hi");
      }
      """;

  private static final String BOOLEAN_IF_CONDITION =
      """
      let flag: boolean = true;
      if (flag) {
        println("hi");
      }
      """;

  private static final String DECLARATION_LEAKING_OUT_OF_BLOCK =
      """
      let flag: boolean = true;
      if (flag) {
        let inner: number = 1;
      }
      println(inner);
      """;

  @Test
  public void rejectsReassigningConstVariable() {
    var diagnostics = Validation.diagnosticsOfV11(CONST_REASSIGNMENT);

    assertEquals(1, diagnostics.size(), EXPECTED_ONE_DIAGNOSTIC);
  }

  @Test
  public void reportsConstReassignmentMessage() {
    var diagnostics = Validation.diagnosticsOfV11(CONST_REASSIGNMENT);

    assertEquals(
        "Cannot assign to const variable 'x'",
        diagnostics.getFirst().message(),
        "expected const-reassignment diagnostic");
  }

  @Test
  public void allowsReassigningLetVariable() {
    var diagnostics = Validation.diagnosticsOfV11(LET_REASSIGNMENT);

    assertTrue(diagnostics.isEmpty(), EXPECTED_NO_DIAGNOSTICS);
  }

  @Test
  public void rejectsNonBooleanIfCondition() {
    var diagnostics = Validation.diagnosticsOfV11(NON_BOOLEAN_IF_CONDITION);

    assertEquals(1, diagnostics.size(), EXPECTED_ONE_DIAGNOSTIC);
  }

  @Test
  public void reportsNonBooleanIfConditionMessage() {
    var diagnostics = Validation.diagnosticsOfV11(NON_BOOLEAN_IF_CONDITION);

    assertEquals(
        "if condition must be boolean, got number",
        diagnostics.getFirst().message(),
        "expected boolean-condition diagnostic");
  }

  @Test
  public void allowsBooleanIfCondition() {
    var diagnostics = Validation.diagnosticsOfV11(BOOLEAN_IF_CONDITION);

    assertTrue(diagnostics.isEmpty(), EXPECTED_NO_DIAGNOSTICS);
  }

  @Test
  public void declarationsInsideBlockDoNotLeakOutsideTheBlock() {
    var diagnostics = Validation.diagnosticsOfV11(DECLARATION_LEAKING_OUT_OF_BLOCK);

    assertEquals(1, diagnostics.size(), EXPECTED_ONE_DIAGNOSTIC);
  }

  @Test
  public void reportsUndeclaredVariableMessageAfterBlockEnds() {
    var diagnostics = Validation.diagnosticsOfV11(DECLARATION_LEAKING_OUT_OF_BLOCK);

    assertEquals(
        "Variable 'inner' is not declared",
        diagnostics.getFirst().message(),
        "expected undeclared-variable diagnostic after the block ends");
  }

  @Test
  public void readInputResolvesToDeclaredVariableType() {
    var diagnostics = Validation.diagnosticsOfV11("let flag: boolean = readInput(\"prompt\");");

    assertTrue(diagnostics.isEmpty(), EXPECTED_NO_DIAGNOSTICS);
  }

  @Test
  public void readInputResolvesToStringWhenUsedAsPrintlnArgument() {
    var diagnostics = Validation.diagnosticsOfV11("println(readInput(\"prompt\"));");

    assertTrue(diagnostics.isEmpty(), EXPECTED_NO_DIAGNOSTICS);
  }

  @Test
  public void rejectsReadInputAsBareStatement() {
    var diagnostics = Validation.diagnosticsOfV11("readInput(\"prompt\");");

    assertEquals(1, diagnostics.size(), EXPECTED_ONE_DIAGNOSTIC);
  }

  @Test
  public void reportsReadInputBareStatementMessage() {
    var diagnostics = Validation.diagnosticsOfV11("readInput(\"prompt\");");

    assertEquals(
        "'readInput' can only be used as a variable initializer or println argument",
        diagnostics.getFirst().message(),
        "expected contextual-typing diagnostic");
  }

  @Test
  public void rejectsReadInputAsBinaryOperand() {
    var diagnostics = Validation.diagnosticsOfV11("let x: number = readInput(\"prompt\") + 1;");

    assertEquals(1, diagnostics.size(), EXPECTED_ONE_DIAGNOSTIC);
  }
}
