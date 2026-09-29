module org.printscript.repl {
  requires org.printscript.lexer;
  requires org.printscript.parser;
  requires org.printscript.typechecker;
  requires org.printscript.typetable;
  requires org.printscript.interpreter;
  requires org.printscript.ast;
  requires org.printscript.tokens;
  requires org.printscript.diagnostics;

  exports org.printscript.repl;
}
