package io.github.simbo1905.mvn2llm;

import java.util.Arrays;
import java.util.logging.Level;

record MainArguments(
    boolean verbose,
    Level logLevel,
    String repo,
    boolean help,
    String coordinate,
    String httpProxy,
    String httpsProxy
) {
  public static final String HTTPS_REPO_1_MAVEN_ORG_MAVEN_2 = "https://repo1.maven.org/maven2";
  private static final String HELP_TEXT = """
      mvn2llm - Maven Download Source JAR And JavaDoc Extraction for LLM Processing
      
      Usage: %s [-v] [-l LEVEL] groupId:artifactId:version
      
      Options:
        -r REPO   Maven repository URL
                  Default: %s
        -v        Enable verbose logging (shorthand for -l FINE)
        -l LEVEL  Set log level (OFF, SEVERE, WARNING, INFO, FINE, FINER, FINEST, ALL)
                  Default: INFO
        -h        Show this help message
        --http-proxy  HTTP proxy URL (overrides HTTP_PROXY environment variable)
        --https-proxy HTTPS proxy URL (overrides HTTPS_PROXY environment variable)
      
      Examples:
        # Normal usage
        %s tech.kwik:kwik:0.9.1
        # Verbose logging
        %s -v com.google.guava:guava:32.1.3-android
        # Disable logging even on errors
        %s -l OFF com.google.guava:guava:32.1.3-jre
      """;

  public boolean proxy() {
    return httpProxy != null || httpsProxy != null;
  }

  static class Builder {
    private boolean verbose = false;
    private boolean help = false;
    private Level logLevel = Level.INFO;
    private String coordinate = null;
    private boolean expectingLevel = false;
    private boolean expectingRepo = false;
    private String repo = HTTPS_REPO_1_MAVEN_ORG_MAVEN_2;
    private String httpProxy = null;
    private String httpsProxy = null;
    private boolean expectingHttpProxy = false;
    private boolean expectingHttpsProxy = false;

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
      if (expectingRepo) {
        this.repo = arg;
        expectingRepo = false;
        return this;
      }
      if (expectingHttpProxy) {
        this.httpProxy = arg;
        expectingHttpProxy = false;
        return this;
      }
      if (expectingHttpsProxy) {
        this.httpsProxy = arg;
        expectingHttpsProxy = false;
        return this;
      }
      return switch (arg) {
        case "-h" -> setHelp();
        case "-v" -> setVerbose();
        case "-l" -> setExpectingLevel();
        case "-r" -> setExpectingRepo();
        case "--http-proxy" -> setExpectingHttpProxy();
        case "--https-proxy" -> setExpectingHttpsProxy();
        default -> setCoordinate(arg);
      };
    }

    Builder setExpectingRepo() {
      this.expectingRepo = true;
      return this;
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

    Builder setExpectingHttpProxy() {
      this.expectingHttpProxy = true;
      return this;
    }

    Builder setExpectingHttpsProxy() {
      this.expectingHttpsProxy = true;
      return this;
    }

    MainArguments toRecord() {
      if (expectingLevel) {
        throw new IllegalArgumentException("Log level not provided after -l flag");
      }
      if (help) {
        return MainArguments.helpInstance();
      }
      if (coordinate == null) {
        throw new IllegalArgumentException("No coordinate provided");
      }
      return new MainArguments(verbose, logLevel, repo, false, coordinate, httpProxy, httpsProxy);
    }
  }

  static MainArguments parse(String[] args) {
    if (args.length == 0) {
      return MainArguments.helpInstance();
    }

    return Arrays.stream(args)
        .reduce(
            new Builder(),
            Builder::process,
            (b1, _) -> b1
        )
        .toRecord();
  }

  private static MainArguments helpInstance() {
    return new MainArguments(false, Level.INFO, HTTPS_REPO_1_MAVEN_ORG_MAVEN_2, true, null, null, null);
  }

  void printHelp() {
    final var isNative = JavaDocExtractor.isNativeImage();
    final var executable = isNative ? "mvn2llm" : "java -jar mvn2llm.jar";
    System.out.printf(HELP_TEXT + "%n", executable, HTTPS_REPO_1_MAVEN_ORG_MAVEN_2, executable, executable, executable);
  }
}
