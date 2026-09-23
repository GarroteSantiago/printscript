package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;
import org.printscript.types.TypeAnnotationTable;
import org.printscript.typetable.BuiltinRegistry;
import org.printscript.typetable.SemanticModel;

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

  private SemanticModel build(String source) {
    var program = TestSources.programOf(source, KeywordTable.v1_1());
    return new SemanticModelBuilder(BuiltinRegistry.v1_1(), TypeAnnotationTable.v1_1())
        .build(program);
  }

  @Test
  void rejectsReassigningConstVariable() {
    var model = build(CONST_REASSIGNMENT);

    assertEquals(1, model.diagnostics().size(), EXPECTED_ONE_DIAGNOSTIC);
  }

  @Test
  void reportsConstReassignmentMessage() {
    var model = build(CONST_REASSIGNMENT);

    assertEquals(
        "Cannot assign to const variable 'x'",
        model.diagnostics().getFirst().message(),
        "expected const-reassignment diagnostic");
  }

  @Test
  void allowsReassigningLetVariable() {
    var model = build(LET_REASSIGNMENT);

    assertTrue(model.diagnostics().isEmpty(), EXPECTED_NO_DIAGNOSTICS);
  }

  @Test
  void rejectsNonBooleanIfCondition() {
    var model = build(NON_BOOLEAN_IF_CONDITION);

    assertEquals(1, model.diagnostics().size(), EXPECTED_ONE_DIAGNOSTIC);
  }

  @Test
  void reportsNonBooleanIfConditionMessage() {
    var model = build(NON_BOOLEAN_IF_CONDITION);

    assertEquals(
        "if condition must be boolean, got number",
        model.diagnostics().getFirst().message(),
        "expected boolean-condition diagnostic");
  }

  @Test
  void allowsBooleanIfCondition() {
    var model = build(BOOLEAN_IF_CONDITION);

    assertTrue(model.diagnostics().isEmpty(), EXPECTED_NO_DIAGNOSTICS);
  }

  @Test
  void declarationsInsideBlockDoNotLeakOutsideTheBlock() {
    var model = build(DECLARATION_LEAKING_OUT_OF_BLOCK);

    assertEquals(1, model.diagnostics().size(), EXPECTED_ONE_DIAGNOSTIC);
  }

  @Test
  void reportsUndeclaredVariableMessageAfterBlockEnds() {
    var model = build(DECLARATION_LEAKING_OUT_OF_BLOCK);

    assertEquals(
        "Variable 'inner' is not declared",
        model.diagnostics().getFirst().message(),
        "expected undeclared-variable diagnostic after the block ends");
  }

  @Test
  void readInputResolvesToDeclaredVariableType() {
    var model = build("let flag: boolean = readInput(\"prompt\");");

    assertTrue(model.diagnostics().isEmpty(), EXPECTED_NO_DIAGNOSTICS);
  }

  @Test
  void readInputResolvesToStringWhenUsedAsPrintlnArgument() {
    var model = build("println(readInput(\"prompt\"));");

    assertTrue(model.diagnostics().isEmpty(), EXPECTED_NO_DIAGNOSTICS);
  }

  @Test
  void rejectsReadInputAsBareStatement() {
    var model = build("readInput(\"prompt\");");

    assertEquals(1, model.diagnostics().size(), EXPECTED_ONE_DIAGNOSTIC);
  }

  @Test
  void reportsReadInputBareStatementMessage() {
    var model = build("readInput(\"prompt\");");

    assertEquals(
        "'readInput' can only be used as a variable initializer or println argument",
        model.diagnostics().getFirst().message(),
        "expected contextual-typing diagnostic");
  }

  @Test
  void rejectsReadInputAsBinaryOperand() {
    var model = build("let x: number = readInput(\"prompt\") + 1;");

    assertEquals(1, model.diagnostics().size(), EXPECTED_ONE_DIAGNOSTIC);
  }
}
