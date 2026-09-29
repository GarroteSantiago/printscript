package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.testkit.TestSources;

class ParserDeclarationTest {
  private VariableDeclarationSyntax declarationOf(String source) {
    var program = TestSources.programOf(source);
    return assertInstanceOf(
        VariableDeclarationSyntax.class,
        program.statements().getFirst(),
        "expected a variable declaration");
  }

  @Test
  public void parsesDeclarationWithoutInitializer() {
    var declaration = declarationOf("let result: number;");

    assertTrue(declaration.initializer().isEmpty(), "expected no initializer");
  }

  @Test
  public void parsesDeclarationWithInitializer() {
    var declaration = declarationOf("let result: number = 5;");

    assertFalse(declaration.initializer().isEmpty(), "expected an initializer");
  }
}
