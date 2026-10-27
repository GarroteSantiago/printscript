package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.ProgramSyntax;
import org.printscript.testkit.TestSources;

class SyntaxTreeBuilderTest {
  @Test
  public void collectsEveryStatementInOrder() {
    ProgramSyntax program =
        new SyntaxTreeBuilder(TestSources.statementsOf("let a: number = 1;\nprintln(a);\na = 2;"))
            .buildProgram();

    assertEquals(3, program.statements().size(), "expected all three statements");
  }

  @Test
  public void anEmptySourceBuildsAnEmptyProgram() {
    ProgramSyntax program = new SyntaxTreeBuilder(TestSources.statementsOf("")).buildProgram();

    assertEquals(0, program.statements().size(), "expected no statements");
  }
}
