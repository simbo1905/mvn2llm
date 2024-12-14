# Maven Source JAR Documentation Extractor

A modern Java 23 utility that downloads source JARs from Maven Central and extracts JavaDoc documentation from them.

## Features

- Downloads source JARs directly from Maven Central based on Maven coordinates e.g. `tech.kwik:kwik:0.9.1`
- Extracts JavaDoc comments from all Java source files adn prints them to stdout in a structured format.
- Does not aim to be totally perfect as LLMs can handle some additional lines below the JavaDoc to avoid missing
  things.a

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
- Does not yet support markdown `/// Some Documentation with [MyClass] links` comments
- Is currently fooled by annotations on methods it is not stripping them out such as `@Override`
- Cannot deal with things like google JARs having an `-android` or java version

## License

This project is open source and available under the MIT License.