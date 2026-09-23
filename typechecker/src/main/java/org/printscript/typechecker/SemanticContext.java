package org.printscript.typechecker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import org.printscript.types.TypeAnnotationTable;
import org.printscript.types.TypeName;
import org.printscript.typetable.BuiltinRegistry;
import org.printscript.typetable.BuiltinSignature;
import org.printscript.typetable.SemanticModel;
import org.printscript.typetable.VariableSymbol;

/**
 * Immutable symbol table plus the per-statement type checker. {@link #validate} never mutates
 * {@code this}: it returns a {@link SemanticStatementResult} carrying the *next* context (with the
 * statement's declarations folded in) so the composition root can validate a program
 * statement-by-statement, interleaved with execution/formatting/analysis, without ever holding a
 * context that reflects a statement that failed to validate. On any diagnostic, the returned next
 * context is simply {@code this} unchanged — a failed statement never contributes symbols.
 *
 * <p>Type checking, symbol resolution, and built-in call resolution here all lean on
 * constructor-injected, swappable strategies ({@link TypeAnnotationTable}, {@link
 * BinaryOperatorRules}, {@link BuiltinRegistry}) rather than hardcoded rules, so a new language
 * version is a new strategy selected by the {@code application} composition root, not a change to
 * this class's dispatch logic.
 */
public final class SemanticContext {
  private final BuiltinRegistry builtins;
  private final TypeAnnotationTable typeAnnotations;
  private final BinaryOperatorRules binaryOperatorRules;
  private final Map<String, VariableSymbol> symbols;

  private SemanticContext(
      BuiltinRegistry builtins,
      TypeAnnotationTable typeAnnotations,
      BinaryOperatorRules binaryOperatorRules,
      Map<String, VariableSymbol> symbols) {
    this.builtins = builtins;
    this.typeAnnotations = typeAnnotations;
    this.binaryOperatorRules = binaryOperatorRules;
    this.symbols = Map.copyOf(symbols);
  }

  public static SemanticContext empty(BuiltinRegistry builtins) {
    return empty(builtins, TypeAnnotationTable.v1());
  }

  public static SemanticContext empty(
      BuiltinRegistry builtins, TypeAnnotationTable typeAnnotations) {
    return empty(builtins, typeAnnotations, BinaryOperatorRules.v1());
  }

  public static SemanticContext empty(
      BuiltinRegistry builtins,
      TypeAnnotationTable typeAnnotations,
      BinaryOperatorRules binaryOperatorRules) {
    return new SemanticContext(builtins, typeAnnotations, binaryOperatorRules, Map.of());
  }

  public SemanticStatementResult validate(StatementSyntax statement) {
    SemanticModel.Builder model = SemanticModel.builder();
    Map<String, VariableSymbol> nextSymbols = new HashMap<>(symbols);
    validateStatement(statement, nextSymbols, model);
    SemanticContext next =
        model.hasErrors()
            ? this
            : new SemanticContext(builtins, typeAnnotations, binaryOperatorRules, nextSymbols);
    SemanticModel semanticModel = model.build();
    return new SemanticStatementResult(next, semanticModel, semanticModel.diagnostics());
  }

  private void validateStatement(
      StatementSyntax statement,
      Map<String, VariableSymbol> nextSymbols,
      SemanticModel.Builder model) {
    statement.accept(new StatementValidator(nextSymbols, model));
  }

  private final class StatementValidator implements StatementVisitor<Void> {
    private final Map<String, VariableSymbol> nextSymbols;
    private final SemanticModel.Builder model;

    StatementValidator(Map<String, VariableSymbol> nextSymbols, SemanticModel.Builder model) {
      this.nextSymbols = nextSymbols;
      this.model = model;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclarationSyntax declaration) {
      String name = declaration.identifier().semanticLexeme();
      TypeName declaredType = typeAnnotations.resolve(declaration.type().semanticLexeme());
      if (nextSymbols.containsKey(name)) {
        model.addDiagnostic(
            error("Variable '" + name + "' is already declared", declaration.span()));
        return null;
      }
      if (declaration.initializer().isEmpty()) {
        if (declaration.isConst()) {
          model.addDiagnostic(
              error("const variable '" + name + "' requires an initializer", declaration.span()));
          return null;
        }
        nextSymbols.put(
            name, new VariableSymbol(name, declaredType, !declaration.isConst(), declaration));
        return null;
      }
      ExpressionSyntax initializer = declaration.initializer().get();
      TypeName initializerType = typeOf(initializer, nextSymbols, model, Optional.of(declaredType));
      if (initializerType == null) return null;
      if (!initializerType.equals(declaredType)) {
        model.addDiagnostic(
            error(
                "Cannot assign " + printable(initializerType) + " to " + printable(declaredType),
                initializer.span()));
        return null;
      }
      nextSymbols.put(
          name, new VariableSymbol(name, declaredType, !declaration.isConst(), declaration));
      return null;
    }

    @Override
    public Void visitAssignment(AssignmentSyntax assignment) {
      String name = assignment.identifier().semanticLexeme();
      VariableSymbol symbol = nextSymbols.get(name);
      if (symbol == null) {
        model.addDiagnostic(error("Variable '" + name + "' is not declared", assignment.span()));
        return null;
      }
      if (!symbol.mutable()) {
        model.addDiagnostic(
            error("Cannot assign to const variable '" + name + "'", assignment.span()));
        return null;
      }
      TypeName valueType =
          typeOf(assignment.value(), nextSymbols, model, Optional.of(symbol.type()));
      if (valueType != null && !valueType.equals(symbol.type())) {
        model.addDiagnostic(
            error(
                "Cannot assign " + printable(valueType) + " to " + printable(symbol.type()),
                assignment.value().span()));
      }
      return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementSyntax expressionStatement) {
      typeOf(expressionStatement.expression(), nextSymbols, model);
      return null;
    }

    @Override
    public Void visitIf(IfStatementSyntax ifStatement) {
      TypeName conditionType = typeOf(ifStatement.condition(), nextSymbols, model);
      if (conditionType != null && !TypeName.BOOLEAN.equals(conditionType)) {
        model.addDiagnostic(
            error(
                "if condition must be boolean, got " + printable(conditionType),
                ifStatement.condition().span()));
      }
      validateBlock(ifStatement.thenBlock(), nextSymbols, model);
      ifStatement.elseBlock().ifPresent(elseBlock -> validateBlock(elseBlock, nextSymbols, model));
      return null;
    }

    @Override
    public Void visitBlock(BlockStatementSyntax block) {
      validateBlock(block, nextSymbols, model);
      return null;
    }
  }

  private void validateBlock(
      BlockStatementSyntax block,
      Map<String, VariableSymbol> outerSymbols,
      SemanticModel.Builder model) {
    Map<String, VariableSymbol> blockSymbols = new HashMap<>(outerSymbols);
    for (StatementSyntax statement : block.statements()) {
      if (model.hasErrors()) return;
      validateStatement(statement, blockSymbols, model);
    }
  }

  private TypeName typeOf(
      ExpressionSyntax expression,
      Map<String, VariableSymbol> symbols,
      SemanticModel.Builder model) {
    return typeOf(expression, symbols, model, Optional.empty());
  }

  private TypeName typeOf(
      ExpressionSyntax expression,
      Map<String, VariableSymbol> symbols,
      SemanticModel.Builder model,
      Optional<TypeName> expected) {
    TypeName type = expression.accept(new ExpressionTyper(symbols, model, expected));
    model.setType(expression, type);
    return type;
  }

  private final class ExpressionTyper implements ExpressionVisitor<TypeName> {
    private final Map<String, VariableSymbol> symbols;
    private final SemanticModel.Builder model;
    private final Optional<TypeName> expected;

    ExpressionTyper(
        Map<String, VariableSymbol> symbols,
        SemanticModel.Builder model,
        Optional<TypeName> expected) {
      this.symbols = symbols;
      this.model = model;
      this.expected = expected;
    }

    @Override
    public TypeName visitLiteral(LiteralExpressionSyntax literal) {
      return literal.literalType();
    }

    @Override
    public TypeName visitIdentifier(IdentifierExpressionSyntax identifier) {
      return identifierType(identifier, symbols, model);
    }

    @Override
    public TypeName visitBinary(BinaryExpressionSyntax binary) {
      return binaryType(binary, symbols, model);
    }

    @Override
    public TypeName visitCall(CallExpressionSyntax call) {
      return callType(call, symbols, model, expected);
    }
  }

  private TypeName identifierType(
      IdentifierExpressionSyntax identifier,
      Map<String, VariableSymbol> symbols,
      SemanticModel.Builder model) {
    String name = identifier.identifier().semanticLexeme();
    VariableSymbol symbol = symbols.get(name);
    if (symbol == null) {
      model.addDiagnostic(error("Variable '" + name + "' is not declared", identifier.span()));
      return null;
    }
    model.resolveVariable(identifier, symbol);
    return symbol.type();
  }

  private TypeName binaryType(
      BinaryExpressionSyntax binary,
      Map<String, VariableSymbol> symbols,
      SemanticModel.Builder model) {
    TypeName left = typeOf(binary.left(), symbols, model);
    if (left == null) return null;
    TypeName right = typeOf(binary.right(), symbols, model);
    if (right == null) return null;
    return binaryOperatorRules
        .resultType(binary.operator().type(), left, right)
        .orElseGet(
            () -> {
              model.addDiagnostic(
                  error(
                      "Operator '"
                          + binary.operator().text()
                          + "' cannot be applied to "
                          + printable(left)
                          + " and "
                          + printable(right),
                      binary.span()));
              return null;
            });
  }

  private TypeName callType(
      CallExpressionSyntax call,
      Map<String, VariableSymbol> symbols,
      SemanticModel.Builder model,
      Optional<TypeName> expected) {
    String callee = call.callee().semanticLexeme();
    BuiltinSignature signature = builtins.find(callee).orElse(null);
    if (signature == null) {
      model.addDiagnostic(error("Unknown callable '" + callee + "'", call.span()));
      return null;
    }
    model.resolveCall(call, signature);
    if (call.arguments().size() != signature.parameterTypes().size()) {
      model.addDiagnostic(
          error(
              "Callable '"
                  + callee
                  + "' expects "
                  + signature.parameterTypes().size()
                  + " argument(s)",
              call.span()));
      return null;
    }
    for (int i = 0; i < call.arguments().size(); i++) {
      TypeName parameterType = signature.parameterTypes().get(i);
      TypeName actual = typeOf(call.arguments().get(i), symbols, model, Optional.of(parameterType));
      if (actual == null) return null;
      if (!signature.printsAnyType() && !actual.equals(parameterType)) {
        model.addDiagnostic(
            error(
                "Callable '"
                    + callee
                    + "' expects "
                    + printable(parameterType)
                    + " but received "
                    + printable(actual),
                call.arguments().get(i).span()));
        return null;
      }
    }
    if (signature.contextual()) {
      if (expected.isEmpty()) {
        model.addDiagnostic(
            error(
                "'" + callee + "' can only be used as a variable initializer or println argument",
                call.span()));
        return null;
      }
      return expected.get();
    }
    return signature.returnType();
  }

  private Diagnostic error(String message, org.printscript.source.SourceSpan span) {
    return Diagnostic.error(Phase.SEMANTIC, message, span);
  }

  private String printable(TypeName typeName) {
    return typeName == null ? "unit" : typeName.toString().toLowerCase(Locale.ROOT);
  }
}
