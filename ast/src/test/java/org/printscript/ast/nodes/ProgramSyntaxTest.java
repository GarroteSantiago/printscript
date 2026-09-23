package org.printscript.ast.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.testkit.TestSources;

class ProgramSyntaxTest {
  @Test
  void spanStartsAtFirstStatementWhenStatementsArePresent() {
    ProgramSyntax program = TestSources.programOf("let a: number = 1;\nprintln(a);\n");

    assertEquals(
        program.statements().getFirst().span().start(),
        program.span().start(),
        "expected span to start at the first statement");
  }

  @Test
  void spanEndsAtEofWhenStatementsArePresent() {
    ProgramSyntax program = TestSources.programOf("let a: number = 1;\nprintln(a);\n");

    assertEquals(program.eof().span().end(), program.span().end(), "expected span to end at eof");
  }

  @Test
  void spanIsEofSpanWhenThereAreNoStatements() {
    ProgramSyntax program = TestSources.programOf("");

    assertEquals(
        program.eof().span(),
        program.span(),
        "expected an empty program's span to be exactly the eof token's span");
  }

  private ProgramSyntax copyBuiltFromMutableList() {
    ProgramSyntax program = TestSources.programOf("println(1);\n");
    List<org.printscript.ast.nodes.statements.StatementSyntax> mutable =
        new ArrayList<>(program.statements());

    ProgramSyntax copy = new ProgramSyntax(mutable, program.eof());
    mutable.clear();
    return copy;
  }

  @Test
  void constructorCopiesTheStatementsListRatherThanAliasingIt() {
    assertEquals(
        1,
        copyBuiltFromMutableList().statements().size(),
        "expected the record's own copy to be unaffected by mutating the caller's list");
  }

  @Test
  void statementsListIsImmutable() {
    ProgramSyntax copy = copyBuiltFromMutableList();

    assertThrows(
        UnsupportedOperationException.class,
        () -> copy.statements().add(null),
        "expected the stored list to be immutable");
  }
}
