package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.printscript.testkit.TestSources;
import org.printscript.typetable.BuiltinRegistry;
import org.printscript.typetable.SemanticModel;

class SemanticModelBuilderTest {
  private SemanticModel semanticModel;

  @BeforeEach
  public void buildModelForInvalidAssignment() {
    var program = TestSources.programOf("let total: number = \"no\";");
    semanticModel = new SemanticModelBuilder(BuiltinRegistry.v1()).build(program);
  }

  @Test
  public void rejectsAssigningStringToNumberVariable() {
    assertEquals(1, semanticModel.diagnostics().size(), "expected exactly one diagnostic");
  }

  @Test
  public void reportsTypeMismatchMessage() {
    assertEquals(
        "Cannot assign string to number",
        semanticModel.diagnostics().getFirst().message(),
        "expected type-mismatch diagnostic message");
  }
}
