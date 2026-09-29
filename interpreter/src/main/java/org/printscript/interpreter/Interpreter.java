package org.printscript.interpreter;

import java.math.BigDecimal;
import org.printscript.ast.nodes.ProgramSyntax;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.ast.nodes.expressions.ExpressionVisitor;
import org.printscript.ast.nodes.expressions.IdentifierExpressionSyntax;
import org.printscript.ast.nodes.expressions.LiteralExpressionSyntax;
import org.printscript.ast.nodes.statements.AssignmentSyntax;
import org.printscript.ast.nodes.statements.BlockStatementSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.IfStatementSyntax;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.ast.nodes.statements.StatementVisitor;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Phase;
import org.printscript.types.TypeName;
import org.printscript.types.TypeNameVisitor;
import org.printscript.typetable.SemanticModel;
import org.printscript.typetable.ValidatedStatement;
import org.printscript.typetable.ValidatedStatementSource;

/**
 * Executes already-validated statements against a {@link SemanticModel}. Runtime state ({@link
 * RuntimeEnvironment}) is immutable: {@link #executeStatement} returns a new environment rather
 * than mutating one in place, so PrintScript variables can still be reassigned while the
 * interpreter's own state is always a value, not a mutable object.
 *
 * <p>This class never re-derives a type decision that {@link SemanticModel} already recorded — e.g.
 * {@code evaluateBinary} reads {@code semanticModel.typeOf(binary)} to decide string concatenation
 * vs. numeric addition, rather than inspecting the runtime values itself. The only per-version
 * strategy this class owns is {@link ArithmeticOperators} (constructor-injected, default {@link
 * ArithmeticOperators#v1()}); everything else version-specific was already resolved by the time a
 * {@link ProgramSyntax}/statement reaches here. I/O is reached only through the {@link
 * OutputPort}/{@link InputPort}/{@link EnvironmentPort} ports, never directly — the 2-argument
 * constructor's defaults reject {@code readInput}/{@code readEnv} with an {@link
 * UnsupportedOperationException} for callers (such as {@code format}/{@code analyze}/{@code
 * validate}) that never wire real I/O in.
 */
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

  /**
   * Drains a {@link ValidatedStatementSource}, executing each statement in turn against the running
   * {@link RuntimeEnvironment}. Lets {@link RuntimeFailure} and {@code typetable.SemanticException}
   * propagate rather than catching them, matching {@link #executeStatement}'s contract — there is
   * no recovery, execution stops at the first one.
   */
  public RuntimeEnvironment executeAll(
      ValidatedStatementSource statements, RuntimeEnvironment initial) {
    RuntimeEnvironment environment = initial;
    while (statements.hasNext()) {
      ValidatedStatement validated = statements.next();
      environment = executeStatement(validated.statement(), environment, validated.model());
    }
    return environment;
  }

  private RuntimeEnvironment execute(
      StatementSyntax statement, RuntimeEnvironment environment, SemanticModel semanticModel) {
    return statement.accept(new StatementExecutor(environment, semanticModel));
  }

  private final class StatementExecutor implements StatementVisitor<RuntimeEnvironment> {
    private final RuntimeEnvironment environment;
    private final SemanticModel semanticModel;

    private StatementExecutor(RuntimeEnvironment environment, SemanticModel semanticModel) {
      this.environment = environment;
      this.semanticModel = semanticModel;
    }

    @Override
    public RuntimeEnvironment visitVariableDeclaration(VariableDeclarationSyntax declaration) {
      return declaration
          .initializer()
          .map(
              initializer ->
                  environment.put(
                      declaration.identifier().semanticLexeme(),
                      evaluate(initializer, environment, semanticModel)))
          .orElse(environment);
    }

    @Override
    public RuntimeEnvironment visitAssignment(AssignmentSyntax assignment) {
      return environment.put(
          assignment.identifier().semanticLexeme(),
          evaluate(assignment.value(), environment, semanticModel));
    }

    @Override
    public RuntimeEnvironment visitExpressionStatement(
        ExpressionStatementSyntax expressionStatement) {
      evaluate(expressionStatement.expression(), environment, semanticModel);
      return environment;
    }

    @Override
    public RuntimeEnvironment visitIf(IfStatementSyntax ifStatement) {
      RuntimeValue condition = evaluate(ifStatement.condition(), environment, semanticModel);
      boolean value = ((RuntimeValue.BooleanValue) condition).value();
      if (value) {
        return executeBlock(ifStatement.thenBlock(), environment, semanticModel);
      } else if (ifStatement.elseBlock().isPresent()) {
        return executeBlock(ifStatement.elseBlock().get(), environment, semanticModel);
      }
      return environment;
    }

    @Override
    public RuntimeEnvironment visitBlock(BlockStatementSyntax block) {
      return executeBlock(block, environment, semanticModel);
    }
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
    return expression.accept(new ExpressionEvaluator(environment, semanticModel));
  }

  private final class ExpressionEvaluator implements ExpressionVisitor<RuntimeValue> {
    private final RuntimeEnvironment environment;
    private final SemanticModel semanticModel;

    private ExpressionEvaluator(RuntimeEnvironment environment, SemanticModel semanticModel) {
      this.environment = environment;
      this.semanticModel = semanticModel;
    }

    @Override
    public RuntimeValue visitLiteral(LiteralExpressionSyntax literal) {
      return literal.literalType().accept(new LiteralValue(literal));
    }

    @Override
    public RuntimeValue visitIdentifier(IdentifierExpressionSyntax identifier) {
      return environment
          .find(identifier.identifier().semanticLexeme())
          .orElseThrow(
              () ->
                  runtime(
                      "Variable '" + identifier.identifier().semanticLexeme() + "' is not declared",
                      identifier));
    }

    @Override
    public RuntimeValue visitBinary(BinaryExpressionSyntax binary) {
      return evaluateBinary(binary, environment, semanticModel);
    }

    @Override
    public RuntimeValue visitCall(CallExpressionSyntax call) {
      return evaluateCall(call, environment, semanticModel);
    }
  }

  private static final class LiteralValue implements TypeNameVisitor<RuntimeValue> {
    private final LiteralExpressionSyntax literal;

    private LiteralValue(LiteralExpressionSyntax literal) {
      this.literal = literal;
    }

    @Override
    public RuntimeValue visitNumber() {
      return new RuntimeValue.NumberValue(new BigDecimal(literal.literal().semanticLexeme()));
    }

    @Override
    public RuntimeValue visitString() {
      return new RuntimeValue.StringValue(literal.literal().semanticLexeme());
    }

    @Override
    public RuntimeValue visitBoolean() {
      return new RuntimeValue.BooleanValue("true".equals(literal.literal().semanticLexeme()));
    }
  }

  private RuntimeValue evaluateBinary(
      BinaryExpressionSyntax binary, RuntimeEnvironment environment, SemanticModel semanticModel) {
    RuntimeValue left = evaluate(binary.left(), environment, semanticModel);
    RuntimeValue right = evaluate(binary.right(), environment, semanticModel);
    if (TypeName.STRING.equals(semanticModel.typeOf(binary).orElse(null))) {
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
    BuiltinBehavior behavior =
        BuiltinBehaviors.find(name)
            .orElseThrow(() -> runtime("Unknown callable '" + name + "'", call));
    return behavior.invoke(argument, new InvocationContext(call, semanticModel));
  }

  private final class InvocationContext implements BuiltinRuntimeContext {
    private final CallExpressionSyntax call;
    private final SemanticModel semanticModel;

    private InvocationContext(CallExpressionSyntax call, SemanticModel semanticModel) {
      this.call = call;
      this.semanticModel = semanticModel;
    }

    @Override
    public CallExpressionSyntax call() {
      return call;
    }

    @Override
    public SemanticModel semanticModel() {
      return semanticModel;
    }

    @Override
    public OutputPort output() {
      return output;
    }

    @Override
    public InputPort input() {
      return input;
    }

    @Override
    public EnvironmentPort env() {
      return env;
    }

    @Override
    public String stringify(RuntimeValue value) {
      return Interpreter.this.stringify(value);
    }

    @Override
    public RuntimeValue parseValue(String raw) {
      return Interpreter.this.parseValue(raw, call, semanticModel);
    }

    @Override
    public RuntimeFailure runtimeFailure(String message) {
      return runtime(message, call);
    }
  }

  private RuntimeValue parseValue(
      String raw, CallExpressionSyntax call, SemanticModel semanticModel) {
    TypeName target =
        semanticModel
            .typeOf(call)
            .orElseThrow(() -> runtime("Could not resolve a type for '" + raw + "'", call));
    return target.accept(new ParsedValue(raw, call));
  }

  private final class ParsedValue implements TypeNameVisitor<RuntimeValue> {
    private final String raw;
    private final CallExpressionSyntax call;

    private ParsedValue(String raw, CallExpressionSyntax call) {
      this.raw = raw;
      this.call = call;
    }

    @Override
    public RuntimeValue visitString() {
      return new RuntimeValue.StringValue(raw);
    }

    @Override
    public RuntimeValue visitNumber() {
      try {
        return new RuntimeValue.NumberValue(new BigDecimal(raw));
      } catch (NumberFormatException exception) {
        RuntimeFailure failure = runtime("Expected a number but got '" + raw + "'", call);
        failure.initCause(exception);
        throw failure;
      }
    }

    @Override
    public RuntimeValue visitBoolean() {
      boolean isTrue = "true".equals(raw);
      boolean isFalse = "false".equals(raw);
      if (isTrue || isFalse) {
        return new RuntimeValue.BooleanValue(isTrue);
      }
      throw runtime("Expected a boolean but got '" + raw + "'", call);
    }
  }

  private String stringify(RuntimeValue value) {
    return value.accept(
        new RuntimeValueVisitor<String>() {
          @Override
          public String visitNumber(RuntimeValue.NumberValue number) {
            return number.value().stripTrailingZeros().toPlainString();
          }

          @Override
          public String visitString(RuntimeValue.StringValue string) {
            return string.value();
          }

          @Override
          public String visitBoolean(RuntimeValue.BooleanValue bool) {
            return String.valueOf(bool.value());
          }

          @Override
          public String visitUnit(RuntimeValue.UnitValue unit) {
            return "";
          }
        });
  }

  private RuntimeFailure runtime(String message, ExpressionSyntax expression) {
    return new RuntimeFailure(Diagnostic.error(Phase.RUNTIME, message, expression.span()));
  }
}
