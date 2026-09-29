package org.printscript.typetable;

import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.types.TypeName;

public record VariableSymbol(
    String name, TypeName type, boolean mutable, VariableDeclarationSyntax declaration) {}
