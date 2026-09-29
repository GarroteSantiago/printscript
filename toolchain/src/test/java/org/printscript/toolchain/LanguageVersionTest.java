package org.printscript.toolchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LanguageVersionTest {
  @Test
  public void parsesAMajorMinorVersion() {
    assertEquals(
        new LanguageVersion(1, 0, 0), LanguageVersion.parse("1.0"), "expected major.minor parsing");
  }

  @Test
  public void parsesAMajorMinorPatchVersion() {
    assertEquals(
        new LanguageVersion(1, 2, 3),
        LanguageVersion.parse("1.2.3"),
        "expected major.minor.patch parsing");
  }

  @Test
  public void rejectsAVersionWithTooFewParts() {
    assertThrows(IllegalArgumentException.class, () -> LanguageVersion.parse("1"));
  }

  @Test
  public void rejectsAVersionWithTooManyParts() {
    assertThrows(IllegalArgumentException.class, () -> LanguageVersion.parse("one.two.three.four"));
  }

  @Test
  public void supportsV1ForVersionOneZero() {
    assertTrue(LanguageVersion.V1_0_0.supportsV1(), "expected 1.0 to support v1");
  }

  @Test
  public void doesNotSupportV1ForVersionOneOne() {
    assertFalse(LanguageVersion.V1_1_0.supportsV1(), "expected 1.1 to not support the v1 flag");
  }

  @Test
  public void supportsV11ForVersionOneOne() {
    assertTrue(LanguageVersion.V1_1_0.supportsV1_1(), "expected 1.1 to support v1.1");
  }

  @Test
  public void doesNotSupportV11ForVersionOneZero() {
    assertFalse(LanguageVersion.V1_0_0.supportsV1_1(), "expected 1.0 to not support v1.1");
  }
}
