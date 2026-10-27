module org.printscript.interpreter {
  requires org.printscript.diagnostics;
  requires transitive org.printscript.ast;
  requires transitive org.printscript.typetable;
  requires transitive org.printscript.types;

  exports org.printscript.interpreter;
}
