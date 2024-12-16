# mvn2llm :: Maven Download Source JAR And JavaDoc Extraction for LLM Processing

```text
                                         
 _____ _____ _____ ___ __    __    _____ 
|     |  |  |   | |_  |  |  |  |  |     |
| | | |  |  | | | |  _|  |__|  |__| | | |
|_|_|_|\___/|_|___|___|_____|_____|_|_|_|
                                         
```

A modern Java 23 command line utility that downloads source JARs from Maven Central and extracts JavaDoc documentation
from them.
It prints output to `stdout` and problems to `stderr`.

## Features

- Downloads source JARs directly from Maven Central based on Maven coordinates e.g. `tech.kwik:kwik:0.9.1`
- Extracts JavaDoc comments from all Java source files adn prints them to stdout in a structured format.
- Does not aim to be totally perfect as LLMs can handle some additional lines below the JavaDoc to avoid missing
  things.
- It does not have a 12M binary like Roaster which is a full Java parser that has an implementation of the eclipse jdt
  in the main jar.

## Requirements

- Java 23 or higher
- No external dependencies required

## Usage

You feed in the correct Maven coordinates and it will download the source JAR and extract the JavaDoc:

```bash
java -jar mvn2llm-0.9-SNAPSHOT.jar <groupId>:<artifactId>:<version>
```

Examples:

```bash
# Normal looking artifact numbering
java -jar target/mvn2llm-0.9-SNAPSHOT.jar tech.kwik:kwik:0.9.1
# Artifact numbering that covers android
java -jar target/mvn2llm-0.9-SNAPSHOT.jar com.google.guava:guava:32.1.3-android
```

The output includes a four line banner. If you are really trying to squeeze those out you can pipe to `tail -n +5`:

```bash
java -jar target/mvn2llm-0.9-SNAPSHOT.jar com.google.guava:guava:32.1.3-android | tail -n +5
```

I would like this to be a cli tool that allows for the unix way of piping and redirecting output rather than add too
any features.

### Output Format

The tool outputs documentation in the following format:

```
File: net.luminis.quic.impl.QuicClientConnectionImpl
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
