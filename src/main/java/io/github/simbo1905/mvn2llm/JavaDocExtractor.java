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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JavaDocExtractor {
  private static final Logger LOGGER = Logger.getLogger(JavaDocExtractor.class.getName());

  public static void main(String[] args) {
    try {
      MainArguments arguments = MainArguments.parse(args);
      if (arguments.help()) {
        arguments.printHelp();
        return;
      }
      configureLogging(arguments.logLevel());
      LOGGER.fine("MainArguments: %s".formatted(arguments));

      // HTTP client should follow redirects
      HttpClient.Builder clientBuilder = HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL);

      // HTTP client should use the proxy if specified
      if (arguments.proxy()) {
        final var proxySelector = ProxyConfig.create(arguments).toProxySelector();
        clientBuilder = clientBuilder.proxy(proxySelector);
      }

      Path sourceFile = null;
      try {
        if (arguments.artefactType() == ArtefactType.JAR) {
          final var mavenCoordinate = MavenCoordinate.parse(arguments.coordinate());
          LOGGER.fine("Parsed mvn coordinate: %s".formatted(mavenCoordinate));
          sourceFile = downloadSourceJar(clientBuilder, arguments.repo(), mavenCoordinate);
        } else {
          LOGGER.fine("Parsed zip url: %s".formatted(arguments.artefactUrl()));
          sourceFile = downloadZipFile(clientBuilder, arguments.artefactUrl());
        }

        final var docs = JavaDocExtractor.extractJavaDocs(sourceFile, arguments.artefactType());
        docs.forEach(System.out::println);
      } finally {
        if (sourceFile != null) {
          Files.deleteIfExists(sourceFile);
          LOGGER.fine("Cleaned up temporary files");
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error processing request", e);
      System.exit(1);
    }
  }

  private static Path downloadZipFile(HttpClient.Builder builder, String url) throws IOException {
    LOGGER.fine("Preparing to download source ZIP for: %s".formatted(url));

    try (final var client = builder.build()) {
      LOGGER.fine("Downloading source ZIP from: %s".formatted(url));

      final var request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .GET()
          .build();

      final var tempFile = Files.createTempFile("maven-source", ".zip");
      LOGGER.fine("Created temporary file: %s".formatted(tempFile));

      try {
        final var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 404) {
          LOGGER.severe("Could not resolve URL. URL not found: " + url);
          throw new IOException("Source ZIP not found");
        } else if (response.statusCode() != 200) {
          throw new IOException("Failed to download ZIP. Status code: " + response.statusCode());
        }

        try (var input = response.body();
             var output = Files.newOutputStream(tempFile)) {
          input.transferTo(output);
          LOGGER.fine("Downloaded source ZIP successfully");
        }
      } catch (Exception e) {
        Files.deleteIfExists(tempFile);
        throw new IOException("Failed to download source ZIP", e);
      }

      return tempFile;
    }
  }

  private static Path downloadSourceJar(
      HttpClient.Builder builder,
      String repo,
      final MavenCoordinate coordinate) throws Exception {
    LOGGER.fine("Preparing to download source JAR for: %s".formatted(coordinate));

    try (final var client = builder.build()) {
      String url;
      if (coordinate.version().endsWith("-SNAPSHOT")) {
        final var snapshot = fetchSnapshotFromServerXml(repo, coordinate, client);

        final var jarName = "%s-%s-sources.jar".formatted(
            coordinate.artifactId(),
            snapshot
        );

        url = "%s/%s/%s/%s/%s".formatted(
            repo,
            coordinate.groupId().replace('.', '/'),
            coordinate.artifactId(),
            coordinate.version(),
            jarName
        );
      } else {
        final var path = coordinate.toPath();
        url = "%s/%s".formatted(repo, path);
      }

      LOGGER.fine("Downloading source JAR from: %s".formatted(url));

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

      return tempFile;
    }
  }

  private static String fetchSnapshotFromServerXml(String repo, MavenCoordinate coordinate, HttpClient client) throws Exception {
    final var metadataUrl = "%s/%s/%s/%s/maven-metadata.xml".formatted(
        repo,
        coordinate.groupId().replace('.', '/'),
        coordinate.artifactId(),
        coordinate.version()
    );

    LOGGER.fine("Fetching metadata from: %s".formatted(metadataUrl));

    final var metadataRequest = HttpRequest.newBuilder()
        .uri(URI.create(metadataUrl))
        .GET()
        .build();

    final var metadataResponse = client.send(metadataRequest, HttpResponse.BodyHandlers.ofString());

    if (metadataResponse.statusCode() != 200) {
      LOGGER.severe("Failed to fetch maven-metadata.xml from: " + metadataUrl);
      throw new IOException("Failed to fetch maven-metadata.xml");
    }

    return SnapshotMetadataParser.parseSnapshotVersion(metadataResponse.body());
  }

  static List<JavaDocInfo> extractJavaDocs(Path artefactPath, ArtefactType artefactType) throws Exception {
    if (artefactType == ArtefactType.JAR) {
      LOGGER.fine("Processing JAR file: %s".formatted(artefactPath));
      final var list = new ArrayList<JavaDocInfo>();
      try (final var jarFile = new JarFile(artefactPath.toFile())) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          final var entry = entries.nextElement();
          LOGGER.fine("Processing entry: %s".formatted(entry.getName()));
          if (entry.getName().endsWith(".java")) {
            LOGGER.fine("Processing Java file: %s".formatted(entry.getName()));
            final var result = extractJavaDocFromEntry(jarFile, entry);
            list.addAll(result);
          }
        }
      }
      return list;
    } else {
      LOGGER.fine("Processing ZIP file: %s".formatted(artefactPath));
      final var list = new ArrayList<JavaDocInfo>();
      try (final var zipFile = new ZipFile(artefactPath.toFile())) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          final var entry = entries.nextElement();
          LOGGER.fine("Processing entry: %s".formatted(entry.getName()));
          if (entry.getName().endsWith(".java")) {
            LOGGER.fine("Processing Java file: %s".formatted(entry.getName()));
            final var result = extractJavaDocFromEntry(zipFile, entry);
            list.addAll(result);
          }
        }
      }
      return list;
    }
  }

  static List<JavaDocInfo> extractJavaDocFromEntry(ZipFile zip, ZipEntry entry) {
    try (final var reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)))) {
      LOGGER.fine("Extracting JavaDoc from: %s".formatted(entry.getName()));
      final LinePushStateMachine stateMachine = new LinePushStateMachine(entry.getName());
      reader.lines().forEach(stateMachine::apply);
      return stateMachine.results;
    } catch (IOException e) {
      LOGGER.warning("Failed to process file %s: %s".formatted(
          entry.getName(), e.getMessage()));
      return Collections.emptyList();
    }
  }

  // Configure logging based on the command line arguments
  private static void configureLogging(Level level) {
    ConsoleHandler handler = new ConsoleHandler();
    LOGGER.setUseParentHandlers(false);
    LOGGER.addHandler(handler);
    LOGGER.setLevel(level);
    handler.setLevel(level);
  }

  // Check if the application is running as a native image to change the help message examples.
  static boolean isNativeImage() {
    try {
      final Class<?> clazz = Class.forName("org.graalvm.nativeimage.ImageInfo");
      try {
        // reflectively invoke the static method inImageRuntimeCode()  on clazz
        final var obj = clazz.getMethod("inImageRuntimeCode").invoke(null);
        return (boolean) obj;
      } catch (Exception e) {
        return false;
      }
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
