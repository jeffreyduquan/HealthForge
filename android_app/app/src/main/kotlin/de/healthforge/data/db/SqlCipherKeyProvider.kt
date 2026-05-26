package de.healthforge.data.db

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates + persists a 32-byte SQLCipher passphrase in
 * EncryptedSharedPreferences (backed by Android Keystore).
 *
 * REQ-PROFILE-001: profile data MUST be stored encrypted at-rest.
 */
@Singleton
class SqlCipherKeyProvider @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "healthforge_db_key",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Returns the 32-byte passphrase (hex-encoded → 64-char string). Generates on first call. */
    fun getOrCreatePassphrase(): ByteArray {
        val hex = prefs.getString(KEY_DB_PASSPHRASE, null)
            ?: generateAndStore()
        return hex.hexToBytes()
    }

    private fun generateAndStore(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hex = bytes.toHex()
        prefs.edit().putString(KEY_DB_PASSPHRASE, hex).apply()
        return hex
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "hex string must have even length" }
        return ByteArray(length / 2) {
            ((Character.digit(this[it * 2], 16) shl 4) +
                Character.digit(this[it * 2 + 1], 16)).toByte()
        }
    }

    private companion object {
        const val KEY_DB_PASSPHRASE = "db_passphrase_hex"
    }
}
