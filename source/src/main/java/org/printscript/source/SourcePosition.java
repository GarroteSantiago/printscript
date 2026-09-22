package org.printscript.source;

/**
 * A single point in source text: 1-based {@code row}/{@code column} for human-facing messages, plus
 * a 0-based {@code offset} for programmatic slicing. All three always move together; nothing in
 * this module resolves one from the other.
 */
public record SourcePosition(int row, int column, int offset) {
  private static final int MIN_ROW = 1;
  private static final int MIN_COLUMN = 1;
  private static final int MIN_OFFSET = 0;

  public SourcePosition {
    if (row < MIN_ROW) throw new IllegalArgumentException("row must be >= 1");
    if (column < MIN_COLUMN) throw new IllegalArgumentException("column must be >= 1");
    if (offset < MIN_OFFSET) throw new IllegalArgumentException("offset must be >= 0");
  }
}
