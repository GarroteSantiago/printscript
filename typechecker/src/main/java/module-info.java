module org.printscript.typechecker {
  requires transitive org.printscript.diagnostics;
  requires transitive org.printscript.ast;
  requires transitive org.printscript.typetable;
  requires transitive org.printscript.types;

  exports org.printscript.typechecker;
}
