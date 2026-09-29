package org.printscript.source;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SourceSpanTest {
  @Test
  public void atFactoryUsesThePositionAsBothStartAndEnd() {
    SourcePosition position = new SourcePosition(2, 4, 7);

    SourceSpan span = SourceSpan.at(position);

    assertEquals(position, span.start(), "expected the position as the start");
  }

  @Test
  public void atFactoryUsesThePositionAsTheEnd() {
    SourcePosition position = new SourcePosition(2, 4, 7);

    SourceSpan span = SourceSpan.at(position);

    assertEquals(position, span.end(), "expected the position as the end");
  }

  @Test
  public void spanCanCoverARangeBetweenTwoPositions() {
    SourcePosition start = new SourcePosition(1, 1, 0);
    SourcePosition end = new SourcePosition(1, 5, 4);

    SourceSpan span = new SourceSpan(start, end);

    assertEquals(end, span.end(), "expected the given end position");
  }
}
