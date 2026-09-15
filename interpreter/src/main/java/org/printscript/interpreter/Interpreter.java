package org.printscript.interpreter;

import java.math.BigDecimal;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Phase;
import org.printscript.semantics.SemanticModel;
import org.printscript.syntax.TypeName;
import org.printscript.syntax.nodes.ProgramSyntax;
import org.printscript.syntax.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.syntax.nodes.expressions.CallExpressionSyntax;
import org.printscript.syntax.nodes.expressions.ExpressionSyntax;
import org.printscript.syntax.nodes.expressions.IdentifierExpressionSyntax;
import org.printscript.syntax.nodes.expressions.LiteralExpressionSyntax;
import org.printscript.syntax.nodes.statements.AssignmentSyntax;
import org.printscript.syntax.nodes.statements.BlockStatementSyntax;
import org.printscript.syntax.nodes.statements.ExpressionStatementSyntax;
import org.printscript.syntax.nodes.statements.IfStatementSyntax;
import org.printscript.syntax.nodes.statements.StatementSyntax;
import org.printscript.syntax.nodes.statements.VariableDeclarationSyntax;

public final class Interpreter {
  private final OutputPort output;
  private final ArithmeticOperators operators;
  private final InputPort input;
  private final EnvironmentPort env;

  public Interpreter(OutputPort output) {
    this(output, ArithmeticOperators.v1());
  }

  public Interpreter(OutputPort output, ArithmeticOperators operators) {
    this(output, operators, unsupportedInput(), unsupportedEnvironment());
  }

  public Interpreter(
      OutputPort output, ArithmeticOperators operators, InputPort input, EnvironmentPort env) {
    this.output = output;
    this.operators = operators;
    this.input = input;
    this.env = env;
  }

  private static InputPort unsupportedInput() {
    return prompt -> {
      throw new UnsupportedOperationException("readInput is not available in this context");
    };
  }

  private static EnvironmentPort unsupportedEnvironment() {
    return name -> {
      throw new UnsupportedOperationException("readEnv is not available in this context");
    };
  }

  public RuntimeEnvironment execute(ProgramSyntax program, SemanticModel semanticModel) {
    RuntimeEnvironment environment = RuntimeEnvironment.empty();
    for (StatementSyntax statement : program.statements()) {
      environment = execute(statement, environment, semanticModel);
    }
    return environment;
  }

  public RuntimeEnvironment executeStatement(
      StatementSyntax statement, RuntimeEnvironment environment, SemanticModel semanticModel) {
    return execute(statement, environment, semanticModel);
  }

  private RuntimeEnvironment execute(
      StatementSyntax statement, RuntimeEnvironment environment, SemanticModel semanticModel) {
    return switch (statement) {
      case VariableDeclarationSyntax declaration ->
          declaration
              .initializer()
              .map(
                  initializer ->
                      environment.put(
                          declaration.identifier().semanticLexeme(),
                          evaluate(initializer, environment, semanticModel)))
              .orElse(environment);
      case AssignmentSyntax assignment ->
          environment.put(
              assignment.identifier().semanticLexeme(),
              evaluate(assignment.value(), environment, semanticModel));
      case ExpressionStatementSyntax expressionStatement -> {
        evaluate(expressionStatement.expression(), environment, semanticModel);
        yield environment;
      }
      case IfStatementSyntax ifStatement -> {
        RuntimeValue condition = evaluate(ifStatement.condition(), environment, semanticModel);
        boolean value = ((RuntimeValue.BooleanValue) condition).value();
        if (value) {
          yield executeBlock(ifStatement.thenBlock(), environment, semanticModel);
        } else if (ifStatement.elseBlock().isPresent()) {
          yield executeBlock(ifStatement.elseBlock().get(), environment, semanticModel);
        } else {
          yield environment;
        }
      }
      case BlockStatementSyntax block -> executeBlock(block, environment, semanticModel);
    };
  }

  private RuntimeEnvironment executeBlock(
      BlockStatementSyntax block, RuntimeEnvironment environment, SemanticModel semanticModel) {
    RuntimeEnvironment current = environment;
    for (StatementSyntax statement : block.statements()) {
      current = execute(statement, current, semanticModel);
    }
    RuntimeEnvironment result = environment;
    for (String name : environment.values().keySet()) {
      result = result.put(name, current.find(name).orElseThrow());
    }
    return result;
  }

  private RuntimeValue evaluate(
      ExpressionSyntax expression, RuntimeEnvironment environment, SemanticModel semanticModel) {
    return switch (expression) {
      case LiteralExpressionSyntax literal ->
          switch (literal.literalType()) {
            case NUMBER ->
                new RuntimeValue.NumberValue(new BigDecimal(literal.literal().semanticLexeme()));
            case STRING -> new RuntimeValue.StringValue(literal.literal().semanticLexeme());
            case BOOLEAN ->
                new RuntimeValue.BooleanValue("true".equals(literal.literal().semanticLexeme()));
          };
      case IdentifierExpressionSyntax identifier ->
          environment
              .find(identifier.identifier().semanticLexeme())
              .orElseThrow(
                  () ->
                      runtime(
                          "Variable '"
                              + identifier.identifier().semanticLexeme()
                              + "' is not declared",
                          identifier));
      case BinaryExpressionSyntax binary -> evaluateBinary(binary, environment, semanticModel);
      case CallExpressionSyntax call -> evaluateCall(call, environment, semanticModel);
    };
  }

  private RuntimeValue evaluateBinary(
      BinaryExpressionSyntax binary, RuntimeEnvironment environment, SemanticModel semanticModel) {
    RuntimeValue left = evaluate(binary.left(), environment, semanticModel);
    RuntimeValue right = evaluate(binary.right(), environment, semanticModel);
    if (semanticModel.typeOf(binary).orElse(null) == TypeName.STRING) {
      return new RuntimeValue.StringValue(stringify(left) + stringify(right));
    }
    BigDecimal leftNumber = ((RuntimeValue.NumberValue) left).value();
    BigDecimal rightNumber = ((RuntimeValue.NumberValue) right).value();
    BigDecimal result;
    try {
      result = operators.apply(binary.operator().type(), leftNumber, rightNumber);
    } catch (ArithmeticException exception) {
      RuntimeFailure failure = runtime(exception.getMessage(), binary);
      failure.initCause(exception);
      throw failure;
    }
    return new RuntimeValue.NumberValue(result);
  }

  private RuntimeValue evaluateCall(
      CallExpressionSyntax call, RuntimeEnvironment environment, SemanticModel semanticModel) {
    if (semanticModel.resolveCall(call).isEmpty()) {
      throw runtime("Unknown callable '" + call.callee().semanticLexeme() + "'", call);
    }
    String name = call.callee().semanticLexeme();
    RuntimeValue argument = evaluate(call.arguments().getFirst(), environment, semanticModel);
    return switch (name) {
      case "println" -> {
        output.println(stringify(argument));
        yield RuntimeValue.UnitValue.INSTANCE;
      }
      case "readInput" -> {
        String raw;
        try {
          raw = input.readLine(stringify(argument));
        } catch (UnsupportedOperationException unsupported) {
          RuntimeFailure failure = runtime(unsupported.getMessage(), call);
          failure.initCause(unsupported);
          throw failure;
        }
        yield parseValue(raw, call, semanticModel);
      }
      case "readEnv" -> {
        String variableName = stringify(argument);
        String raw;
        try {
          raw =
              env.get(variableName)
                  .orElseThrow(
                      () ->
                          runtime("Environment variable '" + variableName + "' is not set", call));
        } catch (UnsupportedOperationException unsupported) {
          RuntimeFailure failure = runtime(unsupported.getMessage(), call);
          failure.initCause(unsupported);
          throw failure;
        }
        yield parseValue(raw, call, semanticModel);
      }
      default -> throw runtime("Unknown callable '" + name + "'", call);
    };
  }

  private RuntimeValue parseValue(
      String raw, CallExpressionSyntax call, SemanticModel semanticModel) {
    TypeName target =
        semanticModel
            .typeOf(call)
            .orElseThrow(() -> runtime("Could not resolve a type for '" + raw + "'", call));
    return switch (target) {
      case STRING -> new RuntimeValue.StringValue(raw);
      case NUMBER -> {
        try {
          yield new RuntimeValue.NumberValue(new BigDecimal(raw));
        } catch (NumberFormatException exception) {
          RuntimeFailure failure = runtime("Expected a number but got '" + raw + "'", call);
          failure.initCause(exception);
          throw failure;
        }
      }
      case BOOLEAN -> {
        boolean isTrue = "true".equals(raw);
        boolean isFalse = "false".equals(raw);
        if (isTrue || isFalse) {
          yield new RuntimeValue.BooleanValue(isTrue);
        }
        throw runtime("Expected a boolean but got '" + raw + "'", call);
      }
    };
  }

  private String stringify(RuntimeValue value) {
    return switch (value) {
      case RuntimeValue.NumberValue number -> number.value().stripTrailingZeros().toPlainString();
      case RuntimeValue.StringValue string -> string.value();
      case RuntimeValue.BooleanValue bool -> String.valueOf(bool.value());
      case RuntimeValue.UnitValue ignored -> "";
    };
  }

  private RuntimeFailure runtime(String message, ExpressionSyntax expression) {
    return new RuntimeFailure(Diagnostic.error(Phase.RUNTIME, message, expression.span()));
  }
}
