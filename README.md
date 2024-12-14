# Maven Source JAR Documentation Extractor

A modern Java 23 utility that downloads source JARs from Maven Central and extracts JavaDoc documentation from them.
This tool is built using modern Java features including records, pattern matching, text blocks, and functional
programming concepts.

## Features

- Downloads source JARs directly from Maven Central based on Maven coordinates e.g. `com.google.guava:guava:32.1.3`
- Extracts JavaDoc comments from all Java source files
- Prints the all the extracted JavaDoc comments for classes and methods to stdout in a structured format.
- It does not aim to be totally perfect as LLMs can handle some additional lines so will take a bit more to avoid
  missing things.

## Requirements

- Java 23 or higher
- No external dependencies required

## Usage

```bash
java -jar mvn2llm-0.9-SNAPSHOT.jar <groupId>:<artifactId>:<version>
```

### Example

```bash
java -jar target/mvn2llm-0.9-SNAPSHOT.jar tech.kwik:kwik:0.9.1
```

### Output Format

The tool outputs documentation in the following format:

```
Class: net.luminis.quic.impl.QuicClientConnectionImpl
JavaDoc:
/**
 * Creates and maintains a QUIC connection with a QUIC server.
 */
public class QuicClientConnectionImpl extends QuicConnectionImpl implements QuicClientConnection, PacketProcessor, TlsStatusEventHandler, FrameProcessor {
```

## Limitations

- Requires direct access to Maven Central
- Source JARs must be available for the requested artifacts
- Does not yet support markdown `/// Some Documentation` comments
- Is currently fooled by annotations on methods it is not stripping them out such as `@Override`
- Cannot deal with things like google guice having an `-android` or java version

## License

This project is open source and available under the MIT License.
