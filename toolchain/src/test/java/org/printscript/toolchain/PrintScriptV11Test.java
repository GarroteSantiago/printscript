package org.printscript.toolchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PrintScriptV11Test {
  private static final String V11_SOURCE =
      """
      const greeting: string = "hi";
      let flag: boolean = true;
      if (flag) {
        println(greeting);
      } else {
        println("bye");
      }
      """;

  private static final String V10_ONLY_SOURCE =
      """
      let name: string = "Joe";
      println(name);
      """;

  private static final String READ_INPUT_AND_READ_ENV_SOURCE =
      """
      let name: string = readInput("Name: ");
      let count: number = readEnv("COUNT");
      println(name);
      println("count: " + count);
      """;

  @Test
  public void executesV11ProgramSuccessfullyUnderV11() {
    CommandResult<ExecutionResult> result =
        new PrintScript().execute(V11_SOURCE, LanguageVersion.V1_1_0, ProgressReporter.NONE);

    assertTrue(result.isSuccess(), "expected v1.1 syntax to succeed under version 1.1");
  }

  @Test
  public void v11ProgramOutputUnderV11() {
    CommandResult<ExecutionResult> result =
        new PrintScript().execute(V11_SOURCE, LanguageVersion.V1_1_0, ProgressReporter.NONE);

    assertEquals(List.of("hi"), result.value().outputLines(), "expected the if-branch output");
  }

  @Test
  public void rejectsV11SyntaxUnderV10() {
    CommandResult<ExecutionResult> result =
        new PrintScript().execute(V11_SOURCE, LanguageVersion.V1_0_0, ProgressReporter.NONE);

    assertFalse(
        result.isSuccess(), "expected v1.1-only syntax to fail when version 1.0 is selected");
  }

  @Test
  public void v10OnlyProgramStillWorksUnderV11() {
    CommandResult<ExecutionResult> result =
        new PrintScript().execute(V10_ONLY_SOURCE, LanguageVersion.V1_1_0, ProgressReporter.NONE);

    assertTrue(result.isSuccess(), "expected a v1.0 program to still work under version 1.1");
  }

  @Test
  public void v10OnlyProgramOutputUnderV11() {
    CommandResult<ExecutionResult> result =
        new PrintScript().execute(V10_ONLY_SOURCE, LanguageVersion.V1_1_0, ProgressReporter.NONE);

    assertEquals(List.of("Joe"), result.value().outputLines(), "expected the println output");
  }

  @Test
  public void readInputAndReadEnvResolveThroughInjectedPorts() {
    List<String> output = new ArrayList<>();

    CommandResult<?> result = executeWithInjectedPorts(output);

    assertTrue(result.isSuccess(), "expected execution to succeed");
  }

  @Test
  public void readInputAndReadEnvProduceExpectedOutput() {
    List<String> output = new ArrayList<>();

    executeWithInjectedPorts(output);

    assertEquals(List.of("Ada", "count: 3"), output, "expected readInput/readEnv resolved output");
  }

  private CommandResult<?> executeWithInjectedPorts(List<String> output) {
    return new PrintScript()
        .execute(
            new StringReader(READ_INPUT_AND_READ_ENV_SOURCE),
            LanguageVersion.V1_1_0,
            output::add,
            prompt -> "Ada",
            name -> "COUNT".equals(name) ? Optional.of("3") : Optional.empty(),
            ProgressReporter.NONE);
  }
}
