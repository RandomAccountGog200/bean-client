#!/bin/sh
# Renders the GUI to build/preview/*.png without launching Minecraft.
# See tools/preview/GuiPreview.java for why this exists.
set -e
cd "$(dirname "$0")/../.."

JDK="/c/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot/bin"
MC="$HOME/.gradle/caches/fabric-loom/26.2/minecraft-client.jar"
LIBS=$(find "$HOME/.gradle/caches/modules-2" \
  \( -name 'gson-2.14.0.jar' -o -name 'joml-1.10.8.jar' -o -name 'slf4j-api-2.0.17.jar' \) \
  2>/dev/null | grep -v sources)

./gradlew classes --offline -q
mkdir -p build/preview-classes

# The JDK here is a Windows build, so it wants Windows paths and ';'.
win() { cygpath -w "$1" 2>/dev/null || echo "$1"; }
CP="$(win build/classes/java/main);$(win "$MC")"
for lib in $LIBS; do CP="$CP;$(win "$lib")"; done

"$JDK/javac" -nowarn -cp "$CP" -d build/preview-classes tools/preview/GuiPreview.java
"$JDK/java" -cp "$CP;$(win build/preview-classes)" GuiPreview "$@"
