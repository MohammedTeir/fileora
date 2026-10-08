plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json) // JsonElement API only; no compiler plugin needed
}

// The tests use a tiny dependency-free harness (see src/test/.../TestHarness.kt) so they run
// identically under Gradle and under scripts/test-core.sh. `gradle check` runs them.
// No JUnit here: the suite is a plain main() run by runAgentTests below.
tasks.named<Test>("test") { enabled = false }

val runAgentTests by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs the agent-core test suite"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.deviceagent.core.AllTestsKt")
}
tasks.named("check") { dependsOn(runAgentTests) }
