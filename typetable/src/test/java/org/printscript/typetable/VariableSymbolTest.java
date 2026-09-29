package org.printscript.typetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.testkit.TestSources;
import org.printscript.types.TypeName;

class VariableSymbolTest {
  private VariableDeclarationSyntax declaration() {
    return (VariableDeclarationSyntax) TestSources.statementsOf("let x: number = 1;").next();
  }

  private VariableSymbol symbol() {
    return new VariableSymbol("x", TypeName.NUMBER, false, declaration());
  }

  @Test
  public void nameReturnsTheConstructedName() {
    assertEquals("x", symbol().name(), "expected name() to return the constructed value");
  }

  @Test
  public void typeReturnsTheConstructedType() {
    assertEquals(
        TypeName.NUMBER, symbol().type(), "expected type() to return the constructed value");
  }

  @Test
  public void mutableReturnsTheConstructedMutability() {
    assertFalse(symbol().mutable(), "expected a 'let' declaration to resolve to immutable");
  }

  @Test
  public void declarationReturnsTheConstructedDeclarationSite() {
    VariableDeclarationSyntax declaration = declaration();

    assertSame(
        declaration,
        new VariableSymbol("x", TypeName.NUMBER, false, declaration).declaration(),
        "expected declaration() to return the exact node it was built from");
  }
}
