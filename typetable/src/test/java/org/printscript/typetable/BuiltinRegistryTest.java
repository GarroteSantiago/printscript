package org.printscript.typetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BuiltinRegistryTest {
  @Test
  void v1FindsPrintln() {
    assertEquals(
        BuiltinRegistry.PRINTLN,
        BuiltinRegistry.v1()
            .find(BuiltinRegistry.PRINTLN)
            .map(BuiltinSignature::name)
            .orElseThrow(),
        "expected v1 to know about println");
  }

  @Test
  void v1DoesNotKnowReadInput() {
    assertTrue(
        BuiltinRegistry.v1().find(BuiltinRegistry.READ_INPUT).isEmpty(),
        "expected readInput to not exist before v1.1");
  }

  @Test
  void v1_1FindsReadInput() {
    assertEquals(
        BuiltinRegistry.READ_INPUT,
        BuiltinRegistry.v1_1()
            .find(BuiltinRegistry.READ_INPUT)
            .map(BuiltinSignature::name)
            .orElseThrow(),
        "expected v1.1 to add readInput");
  }

  @Test
  void v1_1FindsReadEnv() {
    assertEquals(
        BuiltinRegistry.READ_ENV,
        BuiltinRegistry.v1_1()
            .find(BuiltinRegistry.READ_ENV)
            .map(BuiltinSignature::name)
            .orElseThrow(),
        "expected v1.1 to add readEnv");
  }

  @Test
  void findReturnsEmptyForAnUnknownName() {
    assertTrue(
        BuiltinRegistry.v1_1().find("doesNotExist").isEmpty(),
        "expected an unregistered name to resolve to nothing");
  }
}
