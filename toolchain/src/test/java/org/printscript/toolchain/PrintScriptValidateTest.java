package org.printscript.toolchain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PrintScriptValidateTest {
  @Test
  public void validatesAWellFormedProgramSuccessfully() {
    CommandResult<Void> result =
        Commands.validate("let name: string = \"Ada\";\nprintln(name);", LanguageVersion.V1_0_0);

    assertTrue(result.isSuccess(), "expected validation to succeed");
  }

  @Test
  public void reportsASemanticErrorAsADiagnosticFailure() {
    CommandResult<Void> result =
        Commands.validate("let x: number = \"not a number\";", LanguageVersion.V1_0_0);

    assertFalse(result.isSuccess(), "expected a semantic error to fail validation");
  }

  @Test
  public void reportsASyntaxErrorAsADiagnosticFailure() {
    CommandResult<Void> result = Commands.validate("let x number = 1;", LanguageVersion.V1_0_0);

    assertFalse(result.isSuccess(), "expected a syntax error to fail validation");
  }
}
