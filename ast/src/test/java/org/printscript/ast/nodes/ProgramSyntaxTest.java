package org.printscript.ast.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;
import org.printscript.testkit.TestSources;

class ProgramSyntaxTest {
  @Test
  public void spanStartsAtFirstStatementWhenStatementsArePresent() {
    ProgramSyntax program = TestSources.programOf("let a: number = 1;\nprintln(a);\n");

    assertEquals(
        program.statements().getFirst().span().start(),
        program.span().start(),
        "expected span to start at the first statement");
  }

  @Test
  public void spanEndsAtTheLastStatementWhenStatementsArePresent() {
    ProgramSyntax program = TestSources.programOf("let a: number = 1;\nprintln(a);\n");

    assertEquals(
        program.statements().getLast().span().end(),
        program.span().end(),
        "expected span to end at the last statement");
  }

  @Test
  public void spanIsTheStartOfSourceWhenThereAreNoStatements() {
    ProgramSyntax program = TestSources.programOf("");

    assertEquals(
        SourceSpan.at(new SourcePosition(1, 1, 0)),
        program.span(),
        "expected an empty program's span to be the start of the source");
  }

  private ProgramSyntax copyBuiltFromMutableList() {
    ProgramSyntax program = TestSources.programOf("println(1);\n");
    List<org.printscript.ast.nodes.statements.StatementSyntax> mutable =
        new ArrayList<>(program.statements());

    ProgramSyntax copy = new ProgramSyntax(mutable);
    mutable.clear();
    return copy;
  }

  @Test
  public void constructorCopiesTheStatementsListRatherThanAliasingIt() {
    assertEquals(
        1,
        copyBuiltFromMutableList().statements().size(),
        "expected the record's own copy to be unaffected by mutating the caller's list");
  }

  @Test
  public void statementsListIsImmutable() {
    ProgramSyntax copy = copyBuiltFromMutableList();

    assertThrows(
        UnsupportedOperationException.class,
        () -> copy.statements().add(null),
        "expected the stored list to be immutable");
  }
}
