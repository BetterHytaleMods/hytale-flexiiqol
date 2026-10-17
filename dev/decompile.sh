#!/usr/bin/env bash
# Decompile the server jar of the installed Hytale client into
# ~/Hytale-Projects/decompiled/<version>/ for reading the API.
# The output never goes into a repository (Hypixel's code is proprietary).
set -euo pipefail

GAME_JAR="$HOME/Library/Application Support/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"
TOOLS=$HOME/Hytale-Projects/tools/vineflower
OUT_ROOT=$HOME/Hytale-Projects/decompiled
VINEFLOWER_VERSION=1.12.0
JAR=$TOOLS/vineflower-$VINEFLOWER_VERSION.jar

version=$(unzip -p "$GAME_JAR" META-INF/MANIFEST.MF | tr -d '\r' | sed -n 's/^Implementation-Version: //p')
out=$OUT_ROOT/$version
[[ -d $out ]] && { echo "Already decompiled: $out"; exit 0; }

if [[ ! -f $JAR ]]; then
  mkdir -p "$TOOLS"
  url=https://repo1.maven.org/maven2/org/vineflower/vineflower/$VINEFLOWER_VERSION/vineflower-$VINEFLOWER_VERSION.jar
  curl -sSfL -o "$JAR.tmp" "$url"
  [[ $(shasum -a 1 "$JAR.tmp" | awk '{print $1}') == "$(curl -sSfL "$url.sha1" | awk '{print $1}')" ]] \
    || { rm -f "$JAR.tmp"; echo "checksum mismatch" >&2; exit 1; }
  mv "$JAR.tmp" "$JAR"
fi

echo "Decompiling server $version (Hytale classes only, takes a few minutes) ..."
java -Xmx6g -jar "$JAR" --silent --folder --only=com/hypixel/hytale "$GAME_JAR" "$out.tmp"
mv "$out.tmp" "$out"
echo "Done: $out"
