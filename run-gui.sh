#!/usr/bin/env bash
# Builds and runs the JavaFX front end.
#
# No Maven/Gradle: the three JavaFX jars this app needs are fetched straight
# from Maven Central (the same artifacts a build tool would pull) into lib/
# the first time this script runs, then javac/java are pointed at them with
# --module-path.
set -euo pipefail
cd "$(dirname "$0")"

JAVAFX_VERSION=26.0.2
LIB_DIR=lib/javafx

os=$(uname -s)
arch=$(uname -m)
case "$os" in
    Darwin) platform="mac" ;;
    Linux)  platform="linux" ;;
    *) echo "Unsupported OS for the prebuilt JavaFX jars: $os" >&2; exit 1 ;;
esac
case "$arch" in
    arm64|aarch64) platform="${platform}-aarch64" ;;
    x86_64)        if [ "$platform" = "mac" ]; then platform="mac"; else platform="linux"; fi ;;
    *) echo "Unsupported CPU architecture for the prebuilt JavaFX jars: $arch" >&2; exit 1 ;;
esac

mkdir -p "$LIB_DIR"
for module in base graphics controls; do
    jar="$LIB_DIR/javafx-$module-$JAVAFX_VERSION-$platform.jar"
    if [ ! -f "$jar" ]; then
        url="https://repo1.maven.org/maven2/org/openjfx/javafx-$module/$JAVAFX_VERSION/javafx-$module-$JAVAFX_VERSION-$platform.jar"
        echo "Fetching $(basename "$jar")..."
        curl -fsSL -o "$jar" "$url"
    fi
done

OUT_DIR=out/gui
mkdir -p "$OUT_DIR"
javac --module-path "$LIB_DIR" --add-modules javafx.controls \
    -d "$OUT_DIR" gasstation/*.java gasstation/game/*.java gasstation/gui/*.java

java --module-path "$LIB_DIR" --add-modules javafx.controls \
    -cp "$OUT_DIR" gasstation.gui.GasStationTycoonFX "$@"
