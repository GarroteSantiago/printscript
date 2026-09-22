package org.printscript.source;

/**
 * A start/end range in source text, used throughout the pipeline to point at "where in the source
 * did this happen" — a token, an AST node, or a {@link org.printscript.diagnostics.Diagnostic}.
 */
public record SourceSpan(SourcePosition start, SourcePosition end) {
  /**
   * A zero-width span at a single position, for diagnostics that name a point rather than a range.
   */
  public static SourceSpan at(SourcePosition position) {
    return new SourceSpan(position, position);
  }
}
