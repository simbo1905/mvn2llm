# mvn2llm :: Maven Download Source JAR And JavaDoc Extraction for LLM Processing

```text
                                         
 _____ _____ _____ ___ __    __    _____ 
|     |  |  |   | |_  |  |  |  |  |     |
| | | |  |  | | | |  _|  |__|  |__| | | |
|_|_|_|\___/|_|___|___|_____|_____|_|_|_|
                                         
```

A modern Java command line utility that downloads source JARs from Maven Central and extracts JavaDoc documentation
from them. It prints output to `stdout` and problems to `stderr`.

## Features

- Downloads source JARs directly from Maven Central based on Maven coordinates e.g. `tech.kwik:kwik:0.9.1`
- Extracts JavaDoc comments from all Java source files adn prints them to stdout in a simple test file like format.
- No external dependencies required.
- Tiny footprint.

## Requirements

- Java 23 or higher
- No external dependencies required

## Usage

You feed in the correct Maven coordinates and it will download the source JAR and extract the JavaDoc:

```bash
java -jar mvn2llm.jar <groupId>:<artifactId>:<version>
```

Examples:

```
# Get help
java -jar target/mvn2llm.jar -h
# Normal looking artifact numbering
java -jar target/mvn2llm.jar tech.kwik:kwik:0.9.1
# Artifact numbering that covers android
java -jar target/mvn2llm.jar com.google.guava:guava:32.1.3-android
```

The output includes a four line banner. If you are really trying to squeeze those out you can pipe to `tail -n +5`:

```bash
java -jar target/mvn2llm.jar com.google.guava:guava:32.1.3-android | tail -n +5
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
- Does not yet support markdown `/// Some Documentation with [MyClass] links` comments yet!

## Native Image

You need to install GraalVM or higher with the native image plugin. On macOS you can:

```bash
sdk install java 23.0.1-graal
export $GRAALVM_HOME=/Users/xxxx/.sdkman/candidates/java/23.0.1-graal
```

Then you can compile the native image with the following command (which takes a few minutes):

```bash
./native-image-compile.sh
```

You can pass in an option `--all` to attempt to cross compile.

Timing the execution of the native image and the JVM version:

```bash
java -jar target/mvn2llm.jar tech.kwik:kwik:0.9.1  2.26s user 0.28s system 158% cpu 1.602 total
./mvn2llm tech.kwik:kwik:0.9.1  0.04s user 0.03s system 24% cpu 0.301 total
```

The native image is about 5.3x faster (1.602s vs 0.301s) than the JVM version.

## License

This project is open source and available under the MIT License.
