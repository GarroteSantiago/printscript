package org.printscript.typetable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;
import org.printscript.testkit.TestSources;

class ValidatedStatementSourceTest {
  /** A source over a fixed number of statements that counts how many were pulled. */
  private static final class CountingSource implements ValidatedStatementSource {
    private final Deque<ValidatedStatement> remaining = new ArrayDeque<>();
    private int pulled;

    private CountingSource(int statements) {
      for (int i = 0; i < statements; i++) {
        remaining.add(
            new ValidatedStatement(
                TestSources.statementsOf("println(1);").next(), SemanticModel.builder().build()));
      }
    }

    @Override
    public boolean hasNext() {
      return !remaining.isEmpty();
    }

    @Override
    public ValidatedStatement next() {
      pulled++;
      return remaining.pop();
    }
  }

  @Test
  public void drainPullsEveryStatement() {
    CountingSource source = new CountingSource(3);
    source.drain();

    assertEquals(3, source.pulled, "expected drain() to pull all three statements");
  }

  @Test
  public void drainOnAnEmptySourcePullsNothing() {
    CountingSource source = new CountingSource(0);
    source.drain();

    assertEquals(0, source.pulled, "expected drain() not to pull from an empty source");
  }
}
