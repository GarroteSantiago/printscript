package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemanticContextDeclarationTest {
  @Test
  public void allowsDeclarationWithoutInitializerFollowedByAssignment() {
    var diagnostics =
        Validation.diagnosticsOf(
            """
            let result: number;
            result = 5;
            """);

    assertTrue(diagnostics.isEmpty(), "expected no diagnostics");
  }

  @Test
  public void rejectsConstDeclarationWithoutInitializer() {
    var diagnostics = Validation.diagnosticsOfV11("const x: number;");

    assertEquals(1, diagnostics.size(), "expected one diagnostic");
  }

  @Test
  public void reportsConstWithoutInitializerMessage() {
    var diagnostics = Validation.diagnosticsOfV11("const x: number;");

    assertEquals(
        "const variable 'x' requires an initializer",
        diagnostics.getFirst().message(),
        "expected a missing-initializer diagnostic for const");
  }

  @Test
  public void allowsPrintlnCalledDirectlyWithANumber() {
    var diagnostics =
        Validation.diagnosticsOf(
            """
            let result: number = 5;
            println(result);
            """);

    assertTrue(diagnostics.isEmpty(), "expected no diagnostics");
  }
}
