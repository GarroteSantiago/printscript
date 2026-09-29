package org.printscript.repl;

import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.lexer.KeywordTable;
import org.printscript.lexer.Lexer;
import org.printscript.parser.StatementSyntaxReader;
import org.printscript.tokens.SyntaxException;

/**
 * The REPL adapter: a second, thin composition root alongside {@code cli}, reading statements
 * directly from a {@link Reader} instead of a whole source file. Depends on the language core
 * ({@code lexer}, {@code parser}, {@code typechecker}, {@code interpreter}) directly rather than on
 * {@code toolchain.PrintScript} — {@code PrintScript}'s public methods each consume a whole program
 * in one call, which has no natural place to thread state back out between statements; this adapter
 * needs exactly the per-statement primitives ({@code SemanticContext#validate}, {@code
 * Interpreter#executeAll}) those methods already build on internally, so it composes them itself
 * instead of asking {@code toolchain} to grow a new, REPL-specific API. All session state (see
 * {@link ReplSession}) lives here, never in the language core.
 *
 * <p>This class, not {@link ReplSession}, owns the {@code Lexer}/{@code StatementSyntaxReader} pair
 * and is responsible for recovering after a syntax error: {@code StatementSyntaxReader}'s one-token
 * lookahead can be left sitting on a token that can't start any statement, and re-parsing from that
 * same spot would just fail identically forever rather than reaching the next valid statement (or
 * real EOF). Recovery only rebuilds the {@code StatementSyntaxReader} wrapper, not the {@code
 * Lexer} underneath it: the {@code Lexer} itself is still perfectly positioned to keep scanning
 * right after the character that failed it (it always reads one character past whatever it just
 * classified, including a rejected one, before throwing) — discarding it too, and building a brand
 * new one over the same {@code Reader}, would needlessly throw away that position and lose more of
 * the remaining input than necessary. Even so, the {@code StatementSyntaxReader}'s own
 * already-fetched lookahead tokens are lost on every rebuild, so a single malformed statement can
 * still cost a few subsequent tokens before the read loop resynchronizes — the read loop is
 * guaranteed to make forward progress and eventually reach real EOF, never to hang, but is not a
 * lossless recovery.
 */
public final class Repl {
  private static final String PROMPT = "ps> ";

  private Repl() {}

  public static void main(String[] args) {
    boolean v11 = args.length > 0 && "1.1".equals(args[0]);
    run(new InputStreamReader(System.in, StandardCharsets.UTF_8), v11, System.out, System.err);
  }

  /**
   * Drives the loop over an arbitrary {@link Reader}/{@link PrintStream} trio so this is testable
   * with in-memory streams, not just a real terminal. Blocks on {@code input} exactly as much as
   * the parser needs to — a multi-line {@code if (...) { ... }} block simply causes the next read
   * to wait for another line, the same way it already does for {@code toolchain.PrintScript}
   * reading a whole file.
   */
  public static void run(Reader input, boolean v11, PrintStream out, PrintStream err) {
    KeywordTable keywords = v11 ? KeywordTable.v1_1() : KeywordTable.v1();
    out.print(PROMPT);
    out.flush();

    Lexer lexer = new Lexer(input, keywords);
    StatementSource statements = new StatementSyntaxReader(lexer);
    ReplSession session = new ReplSession(v11, out::println);

    while (statements.hasNext()) {
      StatementSyntax statement;
      try {
        statement = statements.next();
      } catch (SyntaxException exception) {
        err.println(render(exception.diagnostic()));
        statements = new StatementSyntaxReader(lexer);
        out.print(PROMPT);
        out.flush();
        continue;
      }

      List<Diagnostic> diagnostics = session.step(statement);
      for (Diagnostic diagnostic : diagnostics) {
        err.println(render(diagnostic));
      }
      out.print(PROMPT);
      out.flush();
    }
  }

  private static String render(Diagnostic diagnostic) {
    var start = diagnostic.span().start();
    var end = diagnostic.span().end();
    return String.format(
        "%s %s at row %d column %d to row %d column %d: %s",
        diagnostic.severity(),
        diagnostic.phase(),
        start.row(),
        start.column(),
        end.row(),
        end.column(),
        diagnostic.message());
  }
}
