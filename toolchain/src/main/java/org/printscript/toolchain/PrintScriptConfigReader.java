package org.printscript.toolchain;

import java.io.IOException;
import java.nio.file.Path;
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.formatter.FormatterConfigProvider;

/**
 * Port through which {@code format}/{@code analyze} config files are turned into the core config
 * types the formatter and analyzer actually consume. {@link JsonPrintScriptConfigReader} is the
 * only production implementation, reading the JSON config schema documented on it; the interface
 * exists so the config file format is a CLI-adapter decision, not something the core module's types
 * are coupled to.
 */
public interface PrintScriptConfigReader {
  FormatterConfigProvider readFormatterConfig(Path path) throws IOException;

  AnalyzerConfig readAnalyzerConfig(Path path) throws IOException;
}
