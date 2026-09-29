package org.printscript.typetable;

import org.printscript.ast.nodes.statements.StatementSyntax;

/**
 * One statement paired with the {@link SemanticModel} recorded while validating it — what a {@link
 * ValidatedStatementSource} yields per pull.
 */
public record ValidatedStatement(StatementSyntax statement, SemanticModel model) {}
