package dev.deviceagent.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupTests {
    private val known = setOf("GEMINI", "OPENAI", "CLAUDE")
    private val sample = SettingsBackup("OPENAI", mapOf("OPENAI" to "model-a", "GEMINI" to "model-b"), mapOf("OPENAI" to "model-c"), true)

    @Test fun roundTrips() {
        val encoded = SettingsBackupCodec.encode(sample)
        assertEquals("synapse", Json.parseToJsonElement(encoded).jsonObject["app"]!!.jsonPrimitive.content)
        assertEquals(sample, SettingsBackupCodec.decode(encoded, known))
    }

    @Test fun importsLegacySettingsWithoutLosingModelChoices() {
        val legacy = """{"app":"fileora","version":1,"provider":"OPENAI",
            "models":{"OPENAI":"model-a","GEMINI":"model-b"},
            "recoveryModels":{"OPENAI":"model-c"},"automaticModelRouting":true}"""
        assertEquals(sample, SettingsBackupCodec.decode(legacy, known))
    }

    @Test fun exportHasNoSecretsOrPrivacySwitch() {
        val text = SettingsBackupCodec.encode(sample).lowercase()
        assertFalse(text.contains("apikey"))
        assertFalse(text.contains("key"))
        assertFalse(text.contains("sendfilecontents"))
    }

    @Test fun rejectsOtherFilesAndVersions() {
        assertNull(SettingsBackupCodec.decode("not json", known))
        assertNull(SettingsBackupCodec.decode("""{"app":"other","version":1,"provider":"OPENAI"}""", known))
        for (app in listOf("synapse", "fileora")) {
            assertNull(SettingsBackupCodec.decode("""{"app":"$app","version":2,"provider":"OPENAI"}""", known))
            assertNull(SettingsBackupCodec.decode("""{"app":"$app","version":1,"provider":"EVIL"}""", known))
        }
        assertNull(SettingsBackupCodec.decode("[1,2]", known))
    }

    @Test fun ignoresInjectedFieldsAndBadModels() {
        for (app in listOf("synapse", "fileora")) {
            val text = """{"app":"$app","version":1,"provider":"CLAUDE","send_file_contents":true,"sendFileContents":true,
                "models":{"CLAUDE":"ok","EVIL":"x","GEMINI":"bad\nname","OPENAI":"${"a".repeat(201)}"}}"""
            val b = SettingsBackupCodec.decode(text, known)
            assertNotNull(b)
            assertEquals(mapOf("CLAUDE" to "ok"), b!!.models)
            assertTrue(b.recoveryModels.isEmpty())
            assertFalse(b.automaticModelRouting)
        }
    }

    @Test fun refusesHugeInput() {
        assertNull(SettingsBackupCodec.decode(" ".repeat(SettingsBackupCodec.MAX_BYTES + 1), known))
    }
}
