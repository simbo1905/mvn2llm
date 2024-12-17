package io.github.simbo1905.mvn2llm;

/*
 * mvn2llm - Maven Source Documentation Extractor for LLM Processing
 *
 * A modern Java tool that extracts JavaDoc documentation from Maven source JARs
 * and formats it for Large Language Model (LLM) processing.
 *
 * Usage: java io.github.simbo1905.mvn2llm.JavaDocExtractor [-v] groupId:artifactId:version
 * Example: java io.github.simbo1905.mvn2llm.JavaDocExtractor -v com.google.guava:guava:32.1.3
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class JavaDocExtractor {
  private static final Logger LOGGER = Logger.getLogger(JavaDocExtractor.class.getName());
  public static final String HTTPS_REPO_1_MAVEN_ORG_MAVEN_2 = "https://repo1.maven.org/maven2";

  private static void configureLogging(Level level) {
    ConsoleHandler handler = new ConsoleHandler();
    LOGGER.setUseParentHandlers(false);
    LOGGER.addHandler(handler);

    LOGGER.setLevel(level);
    handler.setLevel(level);
  }

  public static void main(String[] args) {
    try {
      Arguments arguments = Arguments.parse(args);
      if (arguments.help()) {
        arguments.printHelp();
        return;
      }
      configureLogging(arguments.logLevel());
      LOGGER.fine("Arguments: %s".formatted(arguments));
      final var mavenCoordinate = MavenCoordinate.parse(arguments.coordinate());
      LOGGER.fine("Parsed coordinate: %s".formatted(mavenCoordinate));

      final var sourceJar = downloadSourceJar(mavenCoordinate);
      try {
        final var extractor = new JavaDocExtractor();
        final var docs = extractor.extractJavaDocs(sourceJar);
        docs.forEach(System.out::println);
      } finally {
        Files.deleteIfExists(sourceJar);
        LOGGER.fine("Cleaned up temporary files");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error processing request", e);
      System.exit(1);
    }
  }

  private static Path downloadSourceJar(MavenCoordinate coordinate) throws Exception {
    final var jarPath = coordinate.toPath();
    final var url = "%s/%s".formatted(HTTPS_REPO_1_MAVEN_ORG_MAVEN_2, jarPath);

    LOGGER.fine("Downloading source JAR from: %s".formatted(url));

    try (final var client = HttpClient.newHttpClient()) {
      final var request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .GET()
          .build();

      final var tempFile = Files.createTempFile("maven-source", ".jar");
      LOGGER.fine("Created temporary file: %s".formatted(tempFile));

      try {
        final var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 404) {
          LOGGER.severe("Could not resolve Maven coordinates. URL not found: " + url);
          throw new IOException("Source JAR not found");
        } else if (response.statusCode() != 200) {
          throw new IOException("Failed to download JAR. Status code: " + response.statusCode());
        }

        try (var input = response.body();
             var output = Files.newOutputStream(tempFile)) {
          input.transferTo(output);
          LOGGER.fine("Downloaded source JAR successfully");
        }
      } catch (Exception e) {
        Files.deleteIfExists(tempFile);
        throw new IOException("Failed to download source JAR", e);
      }
      // as we have managed to resolve the url we will print it out at the top of the output
      System.out.println("""
           _____ _____ _____ ___ __    __     _____
          |     |  |  |   | |_  |  |  |  |   |     |
          | | | |  |  | | | |  _|  |__|  |__ | | | |
          |_|_|_|\\___/|_|___|___|_____|______|_|_|_|
          Downloaded:
          """ + url);
      return tempFile;
    }
  }

  List<JavaDocInfo> extractJavaDocs(Path jarPath) throws Exception {
    LOGGER.fine("Processing JAR file: %s".formatted(jarPath));
    final var list = new ArrayList<JavaDocInfo>();
    try (final var jarFile = new JarFile(jarPath.toFile())) {
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        final var entry = entries.nextElement();
        LOGGER.fine("Processing entry: %s".formatted(entry.getName()));
        if (entry.getName().endsWith(".java")) {
          LOGGER.fine("Processing Java file: %s".formatted(entry.getName()));
          final var result = extractJavaDocFromEntry(jarFile, entry).stream()
              .map(JavaDocInfo::toString)
              .collect(Collectors.joining("\n\n"));
          System.out.println(result);
        }
      }
    }
    return list;
  }

  private List<JavaDocInfo> extractJavaDocFromEntry(JarFile jar, ZipEntry entry) {
    try (final var reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)))) {
      LOGGER.fine("Extracting JavaDoc from: %s".formatted(entry.getName()));

      final LinePushStateMachine stateMachine = new LinePushStateMachine(entry.getName());

      reader.lines()
          .forEach(stateMachine::apply);
      return stateMachine.results;
    } catch (IOException e) {
      LOGGER.warning("Failed to process file %s: %s".formatted(
          entry.getName(), e.getMessage()));
      return Collections.emptyList();
    }
  }
}

record Arguments(boolean verbose, boolean help, Level logLevel, String coordinate) {
  private static final String HELP_TEXT = """
      mvn2llm - Maven Download Source JAR And JavaDoc Extraction for LLM Processing
      
      Usage: java -jar mvn2llm.jar [-v] [-l LEVEL] groupId:artifactId:version
      
      Options:
        -h        Show this help message
        -v        Enable verbose logging (shorthand for -l FINE)
        -l LEVEL  Set log level (OFF, SEVERE, WARNING, INFO, FINE, FINER, FINEST, ALL)
                  Default: INFO
      
      Examples:
        # Normal usage
        java -jar mvn2llm.jar tech.kwik:kwik:0.9.1
        # Verbose logging
        java -jar mvn2llm.jar -v com.google.guava:guava:32.1.3-android
        # Disable logging even on errors
        java -jar mvn2llm.jar -l OFF com.google.guava:guava:32.1.3-jre
      """;

  static class Builder {
    private boolean verbose = false;
    private boolean help = false;
    private Level logLevel = Level.INFO;
    private String coordinate = null;
    private boolean expectingLevel = false;

    Builder process(String arg) {
      if (expectingLevel) {
        try {
          this.logLevel = Level.parse(arg.toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid log level: " + arg);
        }
        expectingLevel = false;
        return this;
      }

      return switch (arg) {
        case "-h" -> setHelp();
        case "-v" -> setVerbose();
        case "-l" -> setExpectingLevel();
        default -> setCoordinate(arg);
      };
    }

    Builder setVerbose() {
      this.verbose = true;
      this.logLevel = Level.FINE;
      return this;
    }

    Builder setHelp() {
      this.help = true;
      return this;
    }

    Builder setExpectingLevel() {
      this.expectingLevel = true;
      return this;
    }

    Builder setCoordinate(String coordinate) {
      if (expectingLevel) {
        throw new IllegalArgumentException("Expected log level but got: " + coordinate);
      }
      this.coordinate = coordinate;
      return this;
    }

    Arguments toRecord() {
      if (expectingLevel) {
        throw new IllegalArgumentException("Log level not provided after -l flag");
      }
      if (help) {
        return new Arguments(false, true, Level.INFO, null);
      }
      if (coordinate == null) {
        throw new IllegalArgumentException("No coordinate provided");
      }
      return new Arguments(verbose, false, logLevel, coordinate);
    }
  }

  static Arguments parse(String[] args) {
    if (args.length == 0) {
      return new Arguments(false, true, Level.INFO, null);
    }

    return Arrays.stream(args)
        .reduce(
            new Builder(),
            Builder::process,
            (b1, _) -> b1
        )
        .toRecord();
  }

  void printHelp() {
    System.out.println(HELP_TEXT);
  }
}

record MavenCoordinate(String groupId, String artifactId, String version) {
  static MavenCoordinate parse(String input) {
    final var parts = input.split(":");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid coordinate format. Expected: groupId:artifactId:version");
    }
    return new MavenCoordinate(parts[0], parts[1], parts[2]);
  }

  String toPath() {
    return "%s/%s/%s/%s-%s-sources.jar".formatted(
        groupId.replace('.', '/'),
        artifactId,
        version,
        artifactId,
        version
    );
  }
}
