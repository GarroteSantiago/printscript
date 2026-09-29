package org.printscript.typetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.types.TypeName;

class BuiltinSignatureTest {
  @Test
  public void canonicalConstructorDefaultsToNonContextual() {
    BuiltinSignature signature =
        new BuiltinSignature("identity", List.of(TypeName.NUMBER), TypeName.NUMBER);

    assertFalse(signature.contextual(), "expected the canonical constructor to default contextual");
  }

  @Test
  public void canonicalConstructorDefaultsToNonAnyType() {
    BuiltinSignature signature =
        new BuiltinSignature("identity", List.of(TypeName.NUMBER), TypeName.NUMBER);

    assertFalse(
        signature.printsAnyType(), "expected the canonical constructor to default printsAnyType");
  }

  @Test
  public void contextualFactoryIsContextual() {
    BuiltinSignature signature = BuiltinSignature.contextual("readInput", List.of(TypeName.STRING));

    assertTrue(signature.contextual(), "expected a contextual builtin");
  }

  @Test
  public void contextualFactoryHasNoFixedReturnType() {
    BuiltinSignature signature = BuiltinSignature.contextual("readInput", List.of(TypeName.STRING));

    assertNull(signature.returnType(), "expected no fixed return type for a contextual builtin");
  }

  @Test
  public void printingFactoryAcceptsAnyPrintableType() {
    BuiltinSignature signature = BuiltinSignature.printing("println", TypeName.STRING);

    assertTrue(signature.printsAnyType(), "expected println to accept any printable type");
  }

  @Test
  public void printingFactoryUsesTheCanonicalParameterType() {
    BuiltinSignature signature = BuiltinSignature.printing("println", TypeName.STRING);

    assertEquals(
        List.of(TypeName.STRING),
        signature.parameterTypes(),
        "expected the canonical parameter type");
  }
}
