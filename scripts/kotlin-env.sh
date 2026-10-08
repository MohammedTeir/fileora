#!/usr/bin/env bash
# Offline Kotlin toolchain built from the jars bundled inside Gradle's lib directory.
# Used only where Maven Central is unreachable; normal builds use Gradle (see README).
GRADLE_LIB="${GRADLE_LIB:-$(dirname "$(readlink -f "$(which gradle)")")/../lib}"
KOTLIN_COMPILER_CP=""
for j in kotlin-compiler-embeddable kotlin-stdlib kotlin-script-runtime kotlin-reflect kotlin-daemon-embeddable trove4j annotations- kotlinx-coroutines-core-jvm; do
  for f in "$GRADLE_LIB"/$j*.jar; do [ -f "$f" ] && KOTLIN_COMPILER_CP="$KOTLIN_COMPILER_CP:$f"; done
done
KOTLIN_COMPILER_CP="${KOTLIN_COMPILER_CP#:}"
LIB_CP=""
for j in kotlin-stdlib kotlinx-coroutines-core-jvm kotlinx-serialization-core-jvm kotlinx-serialization-json-jvm; do
  for f in "$GRADLE_LIB"/$j*.jar; do [ -f "$f" ] && LIB_CP="$LIB_CP:$f"; done
done
LIB_CP="${LIB_CP#:}"
kotlinc_offline() { java -cp "$KOTLIN_COMPILER_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect "$@"; }
