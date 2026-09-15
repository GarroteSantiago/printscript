package org.printscript.semantics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.syntax.TypeName;

class BuiltinSignatureTest {
  @Test
  void canonicalConstructorDefaultsToNonContextual() {
    BuiltinSignature signature =
        new BuiltinSignature("identity", List.of(TypeName.NUMBER), TypeName.NUMBER);

    assertFalse(signature.contextual(), "expected the canonical constructor to default contextual");
  }

  @Test
  void canonicalConstructorDefaultsToNonAnyType() {
    BuiltinSignature signature =
        new BuiltinSignature("identity", List.of(TypeName.NUMBER), TypeName.NUMBER);

    assertFalse(
        signature.printsAnyType(), "expected the canonical constructor to default printsAnyType");
  }

  @Test
  void contextualFactoryIsContextual() {
    BuiltinSignature signature = BuiltinSignature.contextual("readInput", List.of(TypeName.STRING));

    assertTrue(signature.contextual(), "expected a contextual builtin");
  }

  @Test
  void contextualFactoryHasNoFixedReturnType() {
    BuiltinSignature signature = BuiltinSignature.contextual("readInput", List.of(TypeName.STRING));

    assertNull(signature.returnType(), "expected no fixed return type for a contextual builtin");
  }

  @Test
  void printingFactoryAcceptsAnyPrintableType() {
    BuiltinSignature signature = BuiltinSignature.printing("println", TypeName.STRING);

    assertTrue(signature.printsAnyType(), "expected println to accept any printable type");
  }

  @Test
  void printingFactoryUsesTheCanonicalParameterType() {
    BuiltinSignature signature = BuiltinSignature.printing("println", TypeName.STRING);

    assertEquals(
        List.of(TypeName.STRING),
        signature.parameterTypes(),
        "expected the canonical parameter type");
  }
}
