package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.lexer.KeywordTable;
import org.printscript.semantics.BuiltinRegistry;
import org.printscript.semantics.SemanticModel;
import org.printscript.syntax.TypeAnnotationTable;
import org.printscript.testkit.TestSources;

class SemanticContextDeclarationTest {
  private SemanticModel build(String source) {
    var program = TestSources.programOf(source);
    return new SemanticModelBuilder(BuiltinRegistry.v1()).build(program);
  }

  private SemanticModel buildV11(String source) {
    var program = TestSources.programOf(source, KeywordTable.v1_1());
    return new SemanticModelBuilder(BuiltinRegistry.v1_1(), TypeAnnotationTable.v1_1())
        .build(program);
  }

  @Test
  void allowsDeclarationWithoutInitializerFollowedByAssignment() {
    var model = build("""
            let result: number;
            result = 5;
            """);

    assertTrue(model.diagnostics().isEmpty(), "expected no diagnostics");
  }

  @Test
  void rejectsConstDeclarationWithoutInitializer() {
    var model = buildV11("const x: number;");

    assertEquals(1, model.diagnostics().size(), "expected one diagnostic");
  }

  @Test
  void reportsConstWithoutInitializerMessage() {
    var model = buildV11("const x: number;");

    assertEquals(
        "const variable 'x' requires an initializer",
        model.diagnostics().getFirst().message(),
        "expected a missing-initializer diagnostic for const");
  }

  @Test
  void allowsPrintlnCalledDirectlyWithANumber() {
    var model =
        build("""
            let result: number = 5;
            println(result);
            """);

    assertTrue(model.diagnostics().isEmpty(), "expected no diagnostics");
  }
}
