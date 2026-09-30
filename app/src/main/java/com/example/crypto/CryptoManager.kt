package com.example.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.log2
import kotlin.math.pow

object CryptoManager {
  private const val ALGORITHM = "AES"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
  private const val PBKDF2_ITERATIONS = 100_000
  private const val KEY_LENGTH_BITS = 256
  private const val GCM_TAG_LENGTH_BITS = 128
  private const val IV_LENGTH_BYTES = 12
  private const val SALT_LENGTH_BYTES = 16

  private val secureRandom = SecureRandom()

  fun generateSalt(): ByteArray {
    val salt = ByteArray(SALT_LENGTH_BYTES)
    secureRandom.nextBytes(salt)
    return salt
  }

  fun generateIv(): ByteArray {
    val iv = ByteArray(IV_LENGTH_BYTES)
    secureRandom.nextBytes(iv)
    return iv
  }

  fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKey {
    val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
    val keyBytes = factory.generateSecret(spec).encoded
    spec.clearPassword()
    return SecretKeySpec(keyBytes, ALGORITHM)
  }

  fun encrypt(plainText: String, key: SecretKey, iv: ByteArray = generateIv()): Pair<String, String> {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
    val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
    val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
    return Pair(cipherTextBase64, ivBase64)
  }

  fun decrypt(cipherTextBase64: String, ivBase64: String, key: SecretKey): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
    val cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
    val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
    cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
    val decryptedBytes = cipher.doFinal(cipherText)
    return String(decryptedBytes, Charsets.UTF_8)
  }

  fun generatePassword(
    length: Int = 16,
    includeUppercase: Boolean = true,
    includeLowercase: Boolean = true,
    includeDigits: Boolean = true,
    includeSymbols: Boolean = true,
    avoidAmbiguous: Boolean = true,
    customSymbols: String = "!@#$%^&*()-_=+[]{}|;:,.<>?",
    excludedCharacters: String = ""
  ): String {
    val upperBase = if (avoidAmbiguous) "ABCDEFGHJKLMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lowerBase = if (avoidAmbiguous) "abcdefghijkmnopqrstuvwxyz" else "abcdefghijklmnopqrstuvwxyz"
    val digitsBase = if (avoidAmbiguous) "23456789" else "0123456789"
    val symbolsBase = customSymbols.ifBlank { "!@#$%^&*()-_=+[]{}|;:,.<>?" }

    val excludedSet = excludedCharacters.toSet()

    val upper = upperBase.filterNot { it in excludedSet }
    val lower = lowerBase.filterNot { it in excludedSet }
    val digits = digitsBase.filterNot { it in excludedSet }
    val symbols = symbolsBase.filterNot { it in excludedSet }

    var pool = ""
    val mandatory = mutableListOf<Char>()

    if (includeUppercase && upper.isNotEmpty()) {
      pool += upper
      mandatory.add(upper[secureRandom.nextInt(upper.length)])
    }
    if (includeLowercase && lower.isNotEmpty()) {
      pool += lower
      mandatory.add(lower[secureRandom.nextInt(lower.length)])
    }
    if (includeDigits && digits.isNotEmpty()) {
      pool += digits
      mandatory.add(digits[secureRandom.nextInt(digits.length)])
    }
    if (includeSymbols && symbols.isNotEmpty()) {
      pool += symbols
      mandatory.add(symbols[secureRandom.nextInt(symbols.length)])
    }

    if (pool.isEmpty()) {
      val fallback = "abcdefghjkmnpqrstuvwxyz23456789".filterNot { it in excludedSet }
      val safePool = if (fallback.isNotEmpty()) fallback else "abcdefghijklmnopqrstuvwxyz0123456789"
      pool = safePool
      mandatory.add(safePool[secureRandom.nextInt(safePool.length)])
    }

    val finalChars = mutableListOf<Char>()
    finalChars.addAll(mandatory)

    val targetLength = length.coerceAtLeast(mandatory.size)
    for (i in mandatory.size until targetLength) {
      finalChars.add(pool[secureRandom.nextInt(pool.length)])
    }

    finalChars.shuffle(secureRandom)
    return finalChars.joinToString("")
  }

  fun generatePassphrase(wordsCount: Int = 4, separator: String = "-"): String {
    val wordList = listOf(
      "amber", "breeze", "beacon", "castle", "cipher", "cobalt", "delta", "eagle",
      "falcon", "galaxy", "harbor", "island", "jungle", "knight", "lagoon", "matrix",
      "nebula", "orbit", "phoenix", "quartz", "radar", "shadow", "timber", "ultra",
      "vector", "vortex", "whisper", "zenith", "aurora", "banyan", "canyon", "crater",
      "glacier", "horizon", "meteor", "oasis", "plasma", "ridge", "safari", "tundra",
      "vertex", "monarch", "alpine", "blizzard", "cavalier", "dynamo", "eclipse", "frontier"
    )
    val chosen = (1..wordsCount).map {
      wordList[secureRandom.nextInt(wordList.size)]
    }
    val digit = secureRandom.nextInt(900) + 100
    return chosen.joinToString(separator) + separator + digit
  }

  fun calculatePasswordStrength(password: String): PasswordStrengthInfo {
    if (password.isEmpty()) {
      return PasswordStrengthInfo(0, "Empty", 0.0, "Password cannot be empty", "Instant")
    }

    var poolSize = 0
    var hasLower = false
    var hasUpper = false
    var hasDigit = false
    var hasSymbol = false

    for (c in password) {
      when {
        c.isLowerCase() -> hasLower = true
        c.isUpperCase() -> hasUpper = true
        c.isDigit() -> hasDigit = true
        else -> hasSymbol = true
      }
    }

    if (hasLower) poolSize += 26
    if (hasUpper) poolSize += 26
    if (hasDigit) poolSize += 10
    if (hasSymbol) poolSize += 33

    val entropy = if (poolSize > 0) password.length * log2(poolSize.toDouble()) else 0.0

    val crackTime = estimateCrackTime(entropy)

    val (score, label, advice) = when {
      password.length < 8 -> Triple(1, "Very Weak", "Too short. Use at least 12 characters.")
      entropy < 40 -> Triple(2, "Weak", "Add numbers, symbols and mix casing.")
      entropy < 60 -> Triple(3, "Fair", "Reasonable, but consider adding more length.")
      entropy < 80 -> Triple(4, "Strong", "Strong security. Resistant to standard brute-force.")
      else -> Triple(5, "Very Strong", "Excellent entropy. Quantum-resilient grade length.")
    }

    return PasswordStrengthInfo(score, label, entropy, advice, crackTime)
  }

  private fun estimateCrackTime(entropyBits: Double): String {
    if (entropyBits <= 0) return "Instant"
    // Search space S = 2^entropy. Average guesses G = 2^(entropy - 1)
    // At 10 billion (10^10) guesses/sec:
    // log10(seconds) = (entropy - 1) * log10(2) - 10
    val log10Seconds = (entropyBits - 1) * 0.30103 - 10.0
    return when {
      log10Seconds < 0 -> "Instant (< 1 second)"
      log10Seconds < 1.77 -> "${(10.0.pow(log10Seconds)).toInt()} seconds"
      log10Seconds < 3.55 -> "${(10.0.pow(log10Seconds) / 60).toInt()} minutes"
      log10Seconds < 4.93 -> "${(10.0.pow(log10Seconds) / 3600).toInt()} hours"
      log10Seconds < 7.49 -> "${(10.0.pow(log10Seconds) / 86400).toInt()} days"
      log10Seconds < 10.49 -> "${(10.0.pow(log10Seconds) / 31536000).toInt()} years"
      log10Seconds < 13.49 -> "${(10.0.pow(log10Seconds) / (31536000 * 1000)).toInt()} thousand years"
      log10Seconds < 16.49 -> "${(10.0.pow(log10Seconds) / (31536000 * 1_000_000)).toInt()} million years"
      else -> "Centuries (Brute-force impractical)"
    }
  }
}

data class PasswordStrengthInfo(
  val score: Int, // 1 to 5
  val label: String,
  val entropyBits: Double,
  val advice: String,
  val estimatedCrackTime: String = "Instant"
)
