package dev.deviceagent.core

import kotlin.system.exitProcess

fun main() {
    registerFileAndToolTests()
    registerEngineTests()
    registerPromptAndCostTests()
    registerFileReadTests()
    registerAiAndSecurityTests()
    registerModelRoutingTests()
    registerReviewChangesTests()
    val failed = runAllRegistered()
    if (failed > 0) exitProcess(1)
}
