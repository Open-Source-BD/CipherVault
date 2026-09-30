package com.example.data.backup

import android.util.Base64
import com.example.crypto.CryptoManager
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object BackupManager {
  private const val FORMAT_HEADER = "CIPHERVAULT_ENCRYPTED_BACKUP"
  private const val CURRENT_VERSION = 1

  fun exportEncryptedBackup(
    items: List<VaultItem>,
    passphrase: String
  ): String {
    // 1. Convert items into JSON array
    val itemsArray = JSONArray()
    for (item in items) {
      val obj = JSONObject().apply {
        put("title", item.title)
        put("username", item.username)
        put("password", item.password)
        put("website", item.website)
        put("notes", item.notes)
        put("category", item.category.name)
        put("isFavorite", item.isFavorite)
        put("createdAt", item.createdAt)
        put("updatedAt", item.updatedAt)
      }
      itemsArray.put(obj)
    }

    val plainJson = itemsArray.toString()

    // 2. Generate unique backup salt and IV
    val salt = CryptoManager.generateSalt()
    val iv = CryptoManager.generateIv()
    val backupKey = CryptoManager.deriveKey(passphrase.toCharArray(), salt)

    // 3. Encrypt payload with AES-256-GCM
    val (cipherTextBase64, ivBase64) = CryptoManager.encrypt(plainJson, backupKey, iv)
    val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

    // 4. Calculate SHA256 checksum of ciphertext for integrity verification
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(cipherTextBase64.toByteArray(Charsets.UTF_8))
    val checksum = digest.joinToString("") { "%02x".format(it) }

    // 5. Construct outer encrypted container
    val container = JSONObject().apply {
      put("format", FORMAT_HEADER)
      put("version", CURRENT_VERSION)
      put("timestamp", System.currentTimeMillis())
      put("itemCount", items.size)
      put("salt", saltBase64)
      put("iv", ivBase64)
      put("payload", cipherTextBase64)
      put("checksum", checksum)
    }

    return container.toString(2)
  }

  fun importEncryptedBackup(
    backupFileContent: String,
    passphrase: String
  ): Result<List<VaultItem>> {
    return try {
      val container = JSONObject(backupFileContent)

      if (!container.has("format") || container.getString("format") != FORMAT_HEADER) {
        return Result.failure(IllegalArgumentException("Invalid backup file format"))
      }

      val saltBase64 = container.getString("salt")
      val ivBase64 = container.getString("iv")
      val cipherPayload = container.getString("payload")
      val expectedChecksum = container.getString("checksum")

      // Verify checksum
      val md = MessageDigest.getInstance("SHA-256")
      val digest = md.digest(cipherPayload.toByteArray(Charsets.UTF_8))
      val computedChecksum = digest.joinToString("") { "%02x".format(it) }
      if (expectedChecksum != computedChecksum) {
        return Result.failure(IllegalStateException("Backup file checksum mismatch or corruption detected"))
      }

      // Derive key with user passphrase
      val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
      val backupKey = CryptoManager.deriveKey(passphrase.toCharArray(), salt)

      // Decrypt
      val decryptedJson = CryptoManager.decrypt(cipherPayload, ivBase64, backupKey)
      val itemsArray = JSONArray(decryptedJson)

      val resultList = mutableListOf<VaultItem>()
      for (i in 0 until itemsArray.length()) {
        val obj = itemsArray.getJSONObject(i)
        val catName = obj.optString("category", VaultCategory.LOGINS.name)
        val cat = try {
          VaultCategory.valueOf(catName)
        } catch (_: Exception) {
          VaultCategory.LOGINS
        }

        resultList.add(
          VaultItem(
            id = 0, // Auto-generate new primary key in Room
            title = obj.getString("title"),
            username = obj.optString("username", ""),
            password = obj.optString("password", ""),
            website = obj.optString("website", ""),
            notes = obj.optString("notes", ""),
            category = cat,
            isFavorite = obj.optBoolean("isFavorite", false),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            isRevealed = false
          )
        )
      }

      Result.success(resultList)
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Failed to decrypt backup. Incorrect password or invalid file."))
    }
  }
}
