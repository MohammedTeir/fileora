package dev.deviceagent.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.deviceagent.core.security.SecretStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores secrets (the user's API key) encrypted with an AES-256-GCM key that lives in the
 * Android Keystore and never leaves it. Only ciphertext is written to SharedPreferences.
 */
class KeystoreSecretStore(context: Context) : SecretStore {
    private val prefs = context.getSharedPreferences("secrets", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    override fun put(name: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(name, packed).apply()
    }

    override fun get(name: String): String? {
        val packed = prefs.getString(name, null) ?: return null
        return try {
            val (iv, data) = packed.split(":", limit = 2)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            null // key invalidated (e.g. screen lock removed) or data corrupt: treat as "not set"
        }
    }

    override fun remove(name: String) { prefs.edit().remove(name).apply() }

    private companion object {
        const val ALIAS = "device_agent_secrets"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
