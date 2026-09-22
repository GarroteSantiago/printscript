package org.printscript.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TypeAnnotationTableTest {
  @Test
  void v1ResolvesNumber() {
    assertEquals(
        TypeName.NUMBER,
        TypeAnnotationTable.v1().resolve("number"),
        "expected 'number' to resolve to NUMBER");
  }

  @Test
  void v1ResolvesString() {
    assertEquals(
        TypeName.STRING,
        TypeAnnotationTable.v1().resolve("string"),
        "expected 'string' to resolve to STRING");
  }

  @Test
  void v1RejectsBoolean() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TypeAnnotationTable.v1().resolve("boolean"),
        "expected v1 to reject 'boolean', which is only recognized starting in v1.1");
  }

  @Test
  void v1_1ResolvesNumber() {
    assertEquals(
        TypeName.NUMBER,
        TypeAnnotationTable.v1_1().resolve("number"),
        "expected 'number' to resolve to NUMBER");
  }

  @Test
  void v1_1ResolvesString() {
    assertEquals(
        TypeName.STRING,
        TypeAnnotationTable.v1_1().resolve("string"),
        "expected 'string' to resolve to STRING");
  }

  @Test
  void v1_1ResolvesBoolean() {
    assertEquals(
        TypeName.BOOLEAN,
        TypeAnnotationTable.v1_1().resolve("boolean"),
        "expected 'boolean' to resolve to BOOLEAN");
  }

  @Test
  void v1_1RejectsUnknownType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TypeAnnotationTable.v1_1().resolve("unknown"),
        "expected an unrecognized type annotation to be rejected");
  }
}
