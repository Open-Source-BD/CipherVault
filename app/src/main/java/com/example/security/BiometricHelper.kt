package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BiometricHelper {
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "CipherVaultBiometricMasterKey"
  private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"

  fun isBiometricOrDeviceCredentialAvailable(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
  }

  fun authenticate(
    activity: FragmentActivity,
    title: String = "Biometric Verification",
    subtitle: String = "Authenticate to continue",
    description: String? = null,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    val executor = ContextCompat.getMainExecutor(activity)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .apply {
        if (!description.isNullOrEmpty()) {
          setDescription(description)
        }
      }
      .setAllowedAuthenticators(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
      )
      .build()

    val biometricPrompt = BiometricPrompt(
      activity,
      executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          onError(errString.toString())
        }

        override fun onAuthenticationFailed() {
          super.onAuthenticationFailed()
          // Soft failure (e.g. fingerprint not recognized), user can retry
        }
      }
    )

    try {
      biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
      onError("Biometric authentication error: ${e.message}")
    }
  }

  // KeyStore Key Management for hardware-backed master key protection
  private fun getOrCreateKeyStoreKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    if (!keyStore.containsAlias(KEY_ALIAS)) {
      val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        ANDROID_KEYSTORE
      )
      val keyGenParameterSpec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
      keyGenerator.init(keyGenParameterSpec)
      return keyGenerator.generateKey()
    }
    val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
    return entry.secretKey
  }

  fun encryptMasterKeyWithKeyStore(rawMasterKeyBytes: ByteArray): Pair<String, String> {
    val keyStoreKey = getOrCreateKeyStoreKey()
    val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
    cipher.init(Cipher.ENCRYPT_MODE, keyStoreKey)
    val encrypted = cipher.doFinal(rawMasterKeyBytes)
    val iv = cipher.iv
    return Pair(
      Base64.encodeToString(encrypted, Base64.NO_WRAP),
      Base64.encodeToString(iv, Base64.NO_WRAP)
    )
  }

  fun decryptMasterKeyWithKeyStore(encryptedKeyBase64: String, ivBase64: String): SecretKey {
    val keyStoreKey = getOrCreateKeyStoreKey()
    val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
    val encrypted = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)
    cipher.init(Cipher.DECRYPT_MODE, keyStoreKey, GCMParameterSpec(128, iv))
    val decryptedKeyBytes = cipher.doFinal(encrypted)
    return SecretKeySpec(decryptedKeyBytes, "AES")
  }
}
