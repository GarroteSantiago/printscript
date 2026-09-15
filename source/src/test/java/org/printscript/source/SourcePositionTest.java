package org.printscript.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SourcePositionTest {
  @Test
  void keepsTheGivenRowColumnAndOffset() {
    SourcePosition position = new SourcePosition(3, 5, 12);

    assertEquals(3, position.row(), "expected the given row");
  }

  @Test
  void keepsTheGivenColumn() {
    SourcePosition position = new SourcePosition(3, 5, 12);

    assertEquals(5, position.column(), "expected the given column");
  }

  @Test
  void keepsTheGivenOffset() {
    SourcePosition position = new SourcePosition(3, 5, 12);

    assertEquals(12, position.offset(), "expected the given offset");
  }

  @Test
  void rejectsARowBelowOne() {
    assertThrows(IllegalArgumentException.class, () -> new SourcePosition(0, 1, 0));
  }

  @Test
  void rejectsAColumnBelowOne() {
    assertThrows(IllegalArgumentException.class, () -> new SourcePosition(1, 0, 0));
  }

  @Test
  void rejectsANegativeOffset() {
    assertThrows(IllegalArgumentException.class, () -> new SourcePosition(1, 1, -1));
  }
}
