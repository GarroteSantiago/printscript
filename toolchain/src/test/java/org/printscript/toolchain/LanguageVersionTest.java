package org.printscript.toolchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LanguageVersionTest {
  private static final LanguageVersionVisitor<String> NAMER =
      new LanguageVersionVisitor<>() {
        @Override
        public String visitV1_0() {
          return "v1.0";
        }

        @Override
        public String visitV1_1() {
          return "v1.1";
        }
      };

  @Test
  public void parsesAMajorMinorVersionOneZero() {
    assertSame(LanguageVersion.V1_0_0, LanguageVersion.parse("1.0"), "expected 1.0");
  }

  @Test
  public void parsesAMajorMinorPatchVersionOneZero() {
    assertSame(LanguageVersion.V1_0_0, LanguageVersion.parse("1.0.0"), "expected 1.0.0");
  }

  @Test
  public void parsesAMajorMinorVersionOneOne() {
    assertSame(LanguageVersion.V1_1_0, LanguageVersion.parse("1.1"), "expected 1.1");
  }

  @Test
  public void ignoresThePatchNumber() {
    assertSame(LanguageVersion.V1_1_0, LanguageVersion.parse("1.1.7"), "expected any 1.1 patch");
  }

  @Test
  public void rejectsAnUnsupportedVersion() {
    assertThrows(IllegalArgumentException.class, () -> LanguageVersion.parse("9.9"));
  }

  @Test
  public void namesTheRejectedVersionInTheMessage() {
    assertTrue(
        rejectionMessage("9.9").contains("Unsupported PrintScript version: 9.9"),
        "expected the rejected version in the message");
  }

  private static String rejectionMessage(String raw) {
    return assertThrows(IllegalArgumentException.class, () -> LanguageVersion.parse(raw))
        .getMessage();
  }

  @Test
  public void rejectsAVersionWithTooFewParts() {
    assertThrows(IllegalArgumentException.class, () -> LanguageVersion.parse("1"));
  }

  @Test
  public void rejectsAVersionWithTooManyParts() {
    assertThrows(IllegalArgumentException.class, () -> LanguageVersion.parse("1.0.0.0.0"));
  }

  @Test
  public void rejectsANonNumericVersion() {
    assertThrows(IllegalArgumentException.class, () -> LanguageVersion.parse("one.zero"));
  }

  @Test
  public void rejectsAnEmptyPart() {
    assertThrows(IllegalArgumentException.class, () -> LanguageVersion.parse("1."));
  }

  @Test
  public void dispatchesVersionOneZeroToItsVisitorMethod() {
    assertEquals("v1.0", LanguageVersion.V1_0_0.accept(NAMER), "expected visitV1_0");
  }

  @Test
  public void dispatchesVersionOneOneToItsVisitorMethod() {
    assertEquals("v1.1", LanguageVersion.V1_1_0.accept(NAMER), "expected visitV1_1");
  }

  @Test
  public void printsVersionOneZero() {
    assertEquals("1.0.0", LanguageVersion.V1_0_0.toString(), "expected 1.0.0");
  }

  @Test
  public void printsVersionOneOne() {
    assertEquals("1.1.0", LanguageVersion.V1_1_0.toString(), "expected 1.1.0");
  }
}
