package com.sagecrest.standards.conventions;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Finds every Java source in the repository and parses it.
 *
 * <p>Rules about source text need the text. ArchUnit reads bytecode, which answers questions about
 * types and dependencies but cannot see a literal inside a method body, because the compiler has
 * already folded it into a constant pool entry with no record of where it was written.
 *
 * <p>The root is located by walking up until every module directory is present, rather than by
 * assuming a working directory. Surefire runs in the module directory, a developer may run from the
 * repository root, and an IDE may choose a third place.
 */
public final class Sources {

  private static final List<String> MODULES =
      List.of("domain", "application", "infrastructure", "web", "conventions");

  private static final String JAVA = ".java";
  private static final String WORKING_DIRECTORY = "user.dir";
  private static final String PARENT = "..";
  private static final String MSG_UNPARSEABLE = "%s cannot be parsed: %s";
  private static final String MSG_NO_ROOT =
      "the repository root is not above the working directory: ";
  private static final int MAX_DEPTH_UPWARD = 5;

  private static final Path ROOT = locateRoot();

  /** Every Java file in the repository, main sources and test sources alike. */
  public static List<CompilationUnit> all() {
    List<CompilationUnit> parsed = new ArrayList<>();
    for (String module : MODULES) {
      parsed.addAll(parse(ROOT.resolve(module)));
    }
    return List.copyOf(parsed);
  }

  public static Path relativize(CompilationUnit unit) {
    return unit.getStorage().map(storage -> ROOT.relativize(storage.getPath())).orElseThrow();
  }

  private static List<CompilationUnit> parse(Path module) {
    if (!Files.isDirectory(module)) {
      return List.of();
    }
    JavaParser parser =
        new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    try (Stream<Path> files = Files.walk(module)) {
      return files
          .filter(path -> path.toString().endsWith(JAVA))
          .map(path -> parseOne(parser, path))
          .toList();
    } catch (IOException unreadable) {
      throw new UncheckedIOException(unreadable);
    }
  }

  private static CompilationUnit parseOne(JavaParser parser, Path path) {
    try {
      ParseResult<CompilationUnit> result = parser.parse(path);
      return result
          .getResult()
          .orElseThrow(
              () ->
                  new IllegalStateException(MSG_UNPARSEABLE.formatted(path, result.getProblems())));
    } catch (IOException unreadable) {
      throw new UncheckedIOException(unreadable);
    }
  }

  private static Path locateRoot() {
    Path walking = Path.of(System.getProperty(WORKING_DIRECTORY)).toAbsolutePath().normalize();
    for (int step = 0; step <= MAX_DEPTH_UPWARD; step++) {
      Path candidate = walking;
      if (MODULES.stream().allMatch(module -> Files.isDirectory(candidate.resolve(module)))) {
        return candidate;
      }
      walking = walking.resolve(PARENT).normalize();
    }
    throw new IllegalStateException(MSG_NO_ROOT + System.getProperty(WORKING_DIRECTORY));
  }

  private Sources() {}
}
