package org.printscript.typechecker;

import org.printscript.semantics.BuiltinRegistry;
import org.printscript.semantics.SemanticModel;
import org.printscript.syntax.TypeAnnotationTable;
import org.printscript.syntax.nodes.ProgramSyntax;
import org.printscript.syntax.nodes.statements.StatementSyntax;

/**
 * Whole-program convenience over {@link SemanticContext#validate}: folds a {@link ProgramSyntax}'s
 * statements through successive contexts and merges their diagnostics into one {@link
 * SemanticModel}, stopping at the first statement that fails. Like {@code
 * syntax.SyntaxTreeBuilder}, this is not the production validation path — {@code
 * application.PrintScript} validates directly off a {@code StatementSource}, one statement at a
 * time, interleaved with execution/formatting/analysis. Used mainly by this module's own tests.
 */
public final class SemanticModelBuilder {
  private final BuiltinRegistry builtins;
  private final TypeAnnotationTable typeAnnotations;
  private final BinaryOperatorRules binaryOperatorRules;

  public SemanticModelBuilder(BuiltinRegistry builtins) {
    this(builtins, TypeAnnotationTable.v1());
  }

  public SemanticModelBuilder(BuiltinRegistry builtins, TypeAnnotationTable typeAnnotations) {
    this(builtins, typeAnnotations, BinaryOperatorRules.v1());
  }

  public SemanticModelBuilder(
      BuiltinRegistry builtins,
      TypeAnnotationTable typeAnnotations,
      BinaryOperatorRules binaryOperatorRules) {
    this.builtins = builtins;
    this.typeAnnotations = typeAnnotations;
    this.binaryOperatorRules = binaryOperatorRules;
  }

  public SemanticModel build(ProgramSyntax program) {
    SemanticContext context = SemanticContext.empty(builtins, typeAnnotations, binaryOperatorRules);
    SemanticModel.Builder combined = SemanticModel.builder();
    for (StatementSyntax statement : program.statements()) {
      SemanticStatementResult result = context.validate(statement);
      result.diagnostics().forEach(combined::addDiagnostic);
      context = result.nextContext();
      if (!result.isSuccess()) break;
    }
    return combined.build();
  }
}
