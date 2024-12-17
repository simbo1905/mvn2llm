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

# Detect host OS
HOST_OS=$(uname | tr '[:upper:]' '[:lower:]')
case "$HOST_OS" in
    linux*)
        HOST_OS="linux"
        ;;
    darwin*)
        HOST_OS="macos"
        ;;
    msys* | cygwin* | mingw*)
        HOST_OS="windows"
        ;;
    *)
        echo "Unknown OS: $HOST_OS"
        exit 1
        ;;
esac

# Function to build for a specific platform
build_platform() {
    local platform=$1
    local output_name=$2
    local extra_flags=$3

    # Check if we're trying to cross-compile for Windows from a non-Windows host
    if [ "$platform" = "windows-amd64" ] && [ "$HOST_OS" != "windows" ]; then
        echo "Warning: Building Windows executable requires building on Windows host."
        echo "Skipping Windows build..."
        return
    fi

    # Check if we're trying to cross-compile for macOS from a non-macOS host
    if [ "$platform" = "macos-amd64" ] && [ "$HOST_OS" != "macos" ]; then
        echo "Warning: Building macOS binary requires building on macOS host."
        echo "Skipping macOS build..."
        return
    fi

    echo "Building native image for $platform..."

    if [ "$platform" = "$HOST_OS-amd64" ]; then
        # Native build (no --target needed)
        $GRAALVM_HOME/bin/native-image \
            --no-fallback \
            --enable-preview \
            --enable-native-access=ALL-UNNAMED \
            -H:+ReportExceptionStackTraces \
            -H:+AddAllCharsets \
            $extra_flags \
            -jar target/mvn2llm.jar \
            $output_name
    else
        # Cross-compilation (only supported for Linux targets from Linux host)
        $GRAALVM_HOME/bin/native-image \
            --no-fallback \
            --enable-preview \
            --enable-native-access=ALL-UNNAMED \
            -H:+ReportExceptionStackTraces \
            -H:+AddAllCharsets \
            --target=$platform \
            $extra_flags \
            -jar target/mvn2llm.jar \
            $output_name
    fi

    echo "Completed build for $platform"
}

# Parse command line arguments
PLATFORMS=()
while [[ $# -gt 0 ]]; do
    case $1 in
        --windows)
            PLATFORMS+=("windows-amd64")
            shift
            ;;
        --linux)
            PLATFORMS+=("linux-amd64")
            shift
            ;;
        --macos)
            PLATFORMS+=("macos-amd64")
            shift
            ;;
        --all)
            PLATFORMS=("windows-amd64" "linux-amd64" "macos-amd64")
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# If no platforms specified, build for current platform
if [ ${#PLATFORMS[@]} -eq 0 ]; then
    PLATFORMS=("$HOST_OS-amd64")
fi

# Build for each platform
for platform in "${PLATFORMS[@]}"; do
    case $platform in
        "windows-amd64")
            build_platform "windows-amd64" "mvn2llm.exe" "--static"
            ;;
        "linux-amd64")
            build_platform "linux-amd64" "mvn2llm" "--static"
            ;;
        "macos-amd64")
            build_platform "macos-amd64" "mvn2llm" ""
            ;;
    esac
done

echo "All native image compilations complete!"
