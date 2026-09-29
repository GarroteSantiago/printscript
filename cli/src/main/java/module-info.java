module org.example.cli {
  requires org.printscript.toolchain;
  requires org.printscript.diagnostics;
  requires info.picocli;

  opens org.example.cli to
      info.picocli;
}
