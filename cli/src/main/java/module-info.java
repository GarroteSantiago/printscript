module org.example.cli {
  requires org.printscript.application;
  requires org.printscript.diagnostics;
  requires info.picocli;

  opens org.example.cli to
      info.picocli;
}
