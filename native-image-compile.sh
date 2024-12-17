#!/bin/bash
set -e

# Check if GRAALVM_HOME is set
if [ -z "$GRAALVM_HOME" ]; then
    echo "Error: GRAALVM_HOME is not set. Please run install-native-image.sh first"
    exit 1
fi

# Check if the JAR file exists
if [ ! -f "target/mvn2llm.jar" ]; then
    echo "Error: target/mvn2llm.jar not found please run 'mvn package' first"
    exit 1
fi

echo "Building native image for mvn2llm.jar. This takes  4m 46s on my M1..."

# Build the native image
$GRAALVM_HOME/bin/native-image \
    --no-fallback \
    --enable-preview \
    --enable-native-access=ALL-UNNAMED \
    -H:+ReportExceptionStackTraces \
    -H:+AddAllCharsets \
    -jar target/mvn2llm.jar \
    mvn2llm

echo "Native image compilation complete. Binary created as 'mvn2llm'"
