module org.printscript.ast {
  requires transitive org.printscript.tokens;
  requires transitive org.printscript.types;

  exports org.printscript.ast;
  exports org.printscript.ast.nodes;
  exports org.printscript.ast.nodes.expressions;
  exports org.printscript.ast.nodes.statements;
}
