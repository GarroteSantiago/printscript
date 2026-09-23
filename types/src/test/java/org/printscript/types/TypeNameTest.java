package org.printscript.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TypeNameTest {
  @Test
  void numberToStringIsNumber() {
    assertEquals("NUMBER", TypeName.NUMBER.toString(), "expected the NUMBER constant's name");
  }

  @Test
  void stringToStringIsString() {
    assertEquals("STRING", TypeName.STRING.toString(), "expected the STRING constant's name");
  }

  @Test
  void booleanToStringIsBoolean() {
    assertEquals("BOOLEAN", TypeName.BOOLEAN.toString(), "expected the BOOLEAN constant's name");
  }

  @Test
  void numberAcceptDispatchesToVisitNumber() {
    assertEquals(
        "number",
        TypeName.NUMBER.accept(NAMING_VISITOR),
        "expected accept() to dispatch to visitNumber");
  }

  @Test
  void stringAcceptDispatchesToVisitString() {
    assertEquals(
        "string",
        TypeName.STRING.accept(NAMING_VISITOR),
        "expected accept() to dispatch to visitString");
  }

  @Test
  void booleanAcceptDispatchesToVisitBoolean() {
    assertEquals(
        "boolean",
        TypeName.BOOLEAN.accept(NAMING_VISITOR),
        "expected accept() to dispatch to visitBoolean");
  }

  private static final TypeNameVisitor<String> NAMING_VISITOR =
      new TypeNameVisitor<>() {
        @Override
        public String visitNumber() {
          return "number";
        }

        @Override
        public String visitString() {
          return "string";
        }

        @Override
        public String visitBoolean() {
          return "boolean";
        }
      };
}
