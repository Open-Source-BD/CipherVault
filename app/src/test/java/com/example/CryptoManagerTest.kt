package com.example

import com.example.crypto.CryptoManager
import com.example.data.backup.BackupManager
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CryptoManagerTest {

  @Test
  fun testEncryptionAndDecryption() {
    val passphrase = "MySecretMasterPassword123!".toCharArray()
    val salt = CryptoManager.generateSalt()
    val key = CryptoManager.deriveKey(passphrase, salt)

    val secretMessage = "SuperSensitiveBankPassword#999"
    val (cipherText, iv) = CryptoManager.encrypt(secretMessage, key)

    assertNotEquals(secretMessage, cipherText)

    val decrypted = CryptoManager.decrypt(cipherText, iv, key)
    assertEquals(secretMessage, decrypted)
  }

  @Test(expected = Exception::class)
  fun testDecryptionFailsWithWrongKey() {
    val salt = CryptoManager.generateSalt()
    val correctKey = CryptoManager.deriveKey("CorrectPassword123".toCharArray(), salt)
    val wrongKey = CryptoManager.deriveKey("WrongPassword999".toCharArray(), salt)

    val (cipherText, iv) = CryptoManager.encrypt("SecretData", correctKey)
    CryptoManager.decrypt(cipherText, iv, wrongKey)
  }

  @Test
  fun testPasswordGenerator() {
    val password = CryptoManager.generatePassword(
      length = 24,
      includeUppercase = true,
      includeLowercase = true,
      includeDigits = true,
      includeSymbols = true
    )
    assertEquals(24, password.length)
    assertTrue(password.any { it.isUpperCase() })
    assertTrue(password.any { it.isLowerCase() })
    assertTrue(password.any { it.isDigit() })
  }

  @Test
  fun testPasswordGeneratorCustomCharacterSets() {
    // Numeric PIN test
    val pin = CryptoManager.generatePassword(
      length = 6,
      includeUppercase = false,
      includeLowercase = false,
      includeDigits = true,
      includeSymbols = false
    )
    assertEquals(6, pin.length)
    assertTrue(pin.all { it.isDigit() })

    // Excluded characters test
    val excludedChars = "ABCDEF123"
    val passwordWithoutExcluded = CryptoManager.generatePassword(
      length = 20,
      includeUppercase = true,
      includeLowercase = true,
      includeDigits = true,
      includeSymbols = true,
      excludedCharacters = excludedChars
    )
    assertEquals(20, passwordWithoutExcluded.length)
    assertTrue(passwordWithoutExcluded.none { it in excludedChars })

    // Custom symbols test
    val customSymbols = "@#"
    val passwordCustomSymbols = CryptoManager.generatePassword(
      length = 15,
      includeUppercase = false,
      includeLowercase = true,
      includeDigits = false,
      includeSymbols = true,
      customSymbols = customSymbols
    )
    assertEquals(15, passwordCustomSymbols.length)
    assertTrue(passwordCustomSymbols.any { it in customSymbols })
  }

  @Test
  fun testPassphraseGenerator() {
    val passphrase = CryptoManager.generatePassphrase(wordsCount = 5, separator = "-")
    val parts = passphrase.split("-")
    assertEquals(6, parts.size) // 5 words + 1 random digit suffix
  }

  @Test
  fun testPasswordStrengthCalculation() {
    val empty = CryptoManager.calculatePasswordStrength("")
    assertEquals(0, empty.score)
    assertEquals(0.0, empty.entropyBits, 0.01)

    val weak = CryptoManager.calculatePasswordStrength("12345")
    assertEquals(1, weak.score) // Very weak
    assertEquals("Very Weak", weak.label)

    val moderate = CryptoManager.calculatePasswordStrength("Passw0rd123!")
    assertTrue(moderate.score in 2..4)
    assertTrue(moderate.entropyBits > 40.0)

    val strong = CryptoManager.calculatePasswordStrength("xK9#mQ2\$vL8*zP1@wR7&")
    assertEquals(5, strong.score)
    assertEquals("Very Strong", strong.label)
    assertTrue(strong.entropyBits >= 80.0)
    assertTrue(strong.estimatedCrackTime.contains("year") || strong.estimatedCrackTime.contains("Centuries"))
  }

  @Test
  fun testBackupExportAndImport() {
    val items = listOf(
      VaultItem(
        id = 1,
        title = "GitHub",
        username = "alice",
        password = "Token12345!Secure",
        website = "https://github.com",
        category = VaultCategory.LOGINS
      ),
      VaultItem(
        id = 2,
        title = "Secret Server Note",
        notes = "Root SSH key passphrase: alpha-omega",
        category = VaultCategory.SECURE_NOTES
      )
    )

    val backupPassphrase = "BackupEncryptionPassword#2026"
    val backupJson = BackupManager.exportEncryptedBackup(items, backupPassphrase)

    assertTrue(backupJson.contains("CIPHERVAULT_ENCRYPTED_BACKUP"))
    // Ensure raw passwords are not exposed in plaintext in backup
    assertTrue(!backupJson.contains("Token12345!Secure"))

    val restoreResult = BackupManager.importEncryptedBackup(backupJson, backupPassphrase)
    assertTrue(restoreResult.isSuccess)
    val restoredList = restoreResult.getOrThrow()
    assertEquals(2, restoredList.size)
    assertEquals("GitHub", restoredList[0].title)
    assertEquals("Token12345!Secure", restoredList[0].password)
    assertEquals("Secret Server Note", restoredList[1].title)
  }
}
