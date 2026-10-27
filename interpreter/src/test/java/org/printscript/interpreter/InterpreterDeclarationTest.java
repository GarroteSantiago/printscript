package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterpreterDeclarationTest {
  private List<String> run(String source) {
    List<String> output = new ArrayList<>();
    var interpreter = new Interpreter(output::add, ArithmeticOperators.v1());
    Execution.run(interpreter, Execution.validated(source));
    return output;
  }

  @Test
  public void declarationWithoutInitializerCanBeAssignedAndPrinted() {
    List<String> output =
        run(
            """
            let result: number;
            result = 5;
            println(result);
            """);

    assertEquals(List.of("5"), output, "expected the later-assigned value to print");
  }

  @Test
  public void printlnAcceptsANumberDirectly() {
    List<String> output =
        run(
            """
            let result: number = 5 * 5 - 8;
            println(result);
            """);

    assertEquals(List.of("17"), output, "expected the number stringified directly");
  }
}
