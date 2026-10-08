#!/usr/bin/env bash
# Builds and tests agent-core WITHOUT Gradle/Maven Central, using only the Kotlin compiler and
# libraries bundled inside Gradle's own lib directory. Equivalent to `gradle :agent-core:check`.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/kotlin-env.sh
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT
echo "== compiling main"
kotlinc_offline -cp "$LIB_CP" -d "$OUT/main" $(find agent-core/src/main -name '*.kt') 2>&1 | grep -v '^Picked up' || true
echo "== compiling tests"
kotlinc_offline -cp "$LIB_CP:$OUT/main" -d "$OUT/test" $(find agent-core/src/test -name '*.kt') 2>&1 | grep -v '^Picked up' || true
echo "== running"
java -cp "$OUT/main:$OUT/test:$LIB_CP" dev.deviceagent.core.AllTestsKt 2>&1 | grep -v '^Picked up'
exit "${PIPESTATUS[0]}"
