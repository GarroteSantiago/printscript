package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.printscript.semantics.BuiltinRegistry;
import org.printscript.semantics.SemanticModel;
import org.printscript.testkit.TestSources;

class SemanticModelBuilderTest {
  private SemanticModel semanticModel;

  @BeforeEach
  void buildModelForInvalidAssignment() {
    var program = TestSources.programOf("let total: number = \"no\";");
    semanticModel = new SemanticModelBuilder(BuiltinRegistry.v1()).build(program);
  }

  @Test
  void rejectsAssigningStringToNumberVariable() {
    assertEquals(1, semanticModel.diagnostics().size(), "expected exactly one diagnostic");
  }

  @Test
  void reportsTypeMismatchMessage() {
    assertEquals(
        "Cannot assign string to number",
        semanticModel.diagnostics().getFirst().message(),
        "expected type-mismatch diagnostic message");
  }
}
