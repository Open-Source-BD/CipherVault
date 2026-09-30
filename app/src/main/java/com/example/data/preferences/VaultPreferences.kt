package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VaultPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("ciphervault_secure_prefs", Context.MODE_PRIVATE)

  private val _themeMode = MutableStateFlow(loadThemeMode())
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

  private val _screenProtection = MutableStateFlow(prefs.getBoolean(KEY_SCREEN_PROTECTION, false))
  val screenProtection: StateFlow<Boolean> = _screenProtection.asStateFlow()

  private val _biometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
  val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

  private val _autoLockSeconds = MutableStateFlow(prefs.getInt(KEY_AUTOLOCK_SECONDS, 60))
  val autoLockSeconds: StateFlow<Int> = _autoLockSeconds.asStateFlow()

  private val _clipboardClearSeconds = MutableStateFlow(prefs.getInt(KEY_CLIPBOARD_CLEAR_SECONDS, 30))
  val clipboardClearSeconds: StateFlow<Int> = _clipboardClearSeconds.asStateFlow()

  private val _requireBiometricForReveal = MutableStateFlow(prefs.getBoolean(KEY_REQUIRE_BIOMETRIC_REVEAL, true))
  val requireBiometricForReveal: StateFlow<Boolean> = _requireBiometricForReveal.asStateFlow()

  private val _externalDataAccessEnabled = MutableStateFlow(prefs.getBoolean(KEY_EXTERNAL_DATA_ACCESS, false))
  val externalDataAccessEnabled: StateFlow<Boolean> = _externalDataAccessEnabled.asStateFlow()

  private val _runtimeTamperGuardEnabled = MutableStateFlow(prefs.getBoolean(KEY_RUNTIME_TAMPER_GUARD, true))
  val runtimeTamperGuardEnabled: StateFlow<Boolean> = _runtimeTamperGuardEnabled.asStateFlow()

  private val _screenOverlayGuardEnabled = MutableStateFlow(prefs.getBoolean(KEY_SCREEN_OVERLAY_GUARD, true))
  val screenOverlayGuardEnabled: StateFlow<Boolean> = _screenOverlayGuardEnabled.asStateFlow()

  fun isVaultInitialized(): Boolean {
    return prefs.contains(KEY_MASTER_SALT) && prefs.contains(KEY_VERIFIER_CIPHER)
  }

  fun saveMasterVerifier(saltBase64: String, verifierCipherBase64: String, ivBase64: String) {
    prefs.edit()
      .putString(KEY_MASTER_SALT, saltBase64)
      .putString(KEY_VERIFIER_CIPHER, verifierCipherBase64)
      .putString(KEY_VERIFIER_IV, ivBase64)
      .apply()
  }

  fun getMasterSalt(): String? = prefs.getString(KEY_MASTER_SALT, null)
  fun getVerifierCipher(): String? = prefs.getString(KEY_VERIFIER_CIPHER, null)
  fun getVerifierIv(): String? = prefs.getString(KEY_VERIFIER_IV, null)

  fun saveBiometricEncryptedKey(encryptedKeyBase64: String, ivBase64: String) {
    prefs.edit()
      .putString(KEY_BIOMETRIC_KEY, encryptedKeyBase64)
      .putString(KEY_BIOMETRIC_IV, ivBase64)
      .putBoolean(KEY_BIOMETRIC_ENABLED, true)
      .apply()
    _biometricEnabled.value = true
  }

  fun getBiometricEncryptedKey(): Pair<String, String>? {
    val key = prefs.getString(KEY_BIOMETRIC_KEY, null) ?: return null
    val iv = prefs.getString(KEY_BIOMETRIC_IV, null) ?: return null
    return Pair(key, iv)
  }

  fun disableBiometric() {
    prefs.edit()
      .remove(KEY_BIOMETRIC_KEY)
      .remove(KEY_BIOMETRIC_IV)
      .putBoolean(KEY_BIOMETRIC_ENABLED, false)
      .apply()
    _biometricEnabled.value = false
  }

  fun setThemeMode(mode: AppThemeMode) {
    prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    _themeMode.value = mode
  }

  private fun loadThemeMode(): AppThemeMode {
    val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK_SLATE.name)
    return try {
      AppThemeMode.valueOf(name ?: AppThemeMode.DARK_SLATE.name)
    } catch (_: Exception) {
      AppThemeMode.DARK_SLATE
    }
  }

  fun setScreenProtection(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_SCREEN_PROTECTION, enabled).apply()
    _screenProtection.value = enabled
  }

  fun setAutoLockSeconds(seconds: Int) {
    prefs.edit().putInt(KEY_AUTOLOCK_SECONDS, seconds).apply()
    _autoLockSeconds.value = seconds
  }

  fun setClipboardClearSeconds(seconds: Int) {
    prefs.edit().putInt(KEY_CLIPBOARD_CLEAR_SECONDS, seconds).apply()
    _clipboardClearSeconds.value = seconds
  }

  fun setRequireBiometricForReveal(required: Boolean) {
    prefs.edit().putBoolean(KEY_REQUIRE_BIOMETRIC_REVEAL, required).apply()
    _requireBiometricForReveal.value = required
  }

  fun setExternalDataAccessEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_EXTERNAL_DATA_ACCESS, enabled).apply()
    _externalDataAccessEnabled.value = enabled
  }

  fun setRuntimeTamperGuardEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_RUNTIME_TAMPER_GUARD, enabled).apply()
    _runtimeTamperGuardEnabled.value = enabled
  }

  fun setScreenOverlayGuardEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_SCREEN_OVERLAY_GUARD, enabled).apply()
    _screenOverlayGuardEnabled.value = enabled
  }

  fun clearAllData() {
    prefs.edit().clear().apply()
    _biometricEnabled.value = false
    _screenProtection.value = false
    _autoLockSeconds.value = 60
    _externalDataAccessEnabled.value = false
    _runtimeTamperGuardEnabled.value = true
    _screenOverlayGuardEnabled.value = true
  }

  companion object {
    private const val KEY_MASTER_SALT = "key_master_salt"
    private const val KEY_VERIFIER_CIPHER = "key_verifier_cipher"
    private const val KEY_VERIFIER_IV = "key_verifier_iv"
    private const val KEY_BIOMETRIC_KEY = "key_biometric_key"
    private const val KEY_BIOMETRIC_IV = "key_biometric_iv"
    private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_SCREEN_PROTECTION = "key_screen_protection"
    private const val KEY_AUTOLOCK_SECONDS = "key_autolock_seconds"
    private const val KEY_CLIPBOARD_CLEAR_SECONDS = "key_clipboard_clear_seconds"
    private const val KEY_REQUIRE_BIOMETRIC_REVEAL = "key_require_biometric_reveal"
    private const val KEY_EXTERNAL_DATA_ACCESS = "key_external_data_access"
    private const val KEY_RUNTIME_TAMPER_GUARD = "key_runtime_tamper_guard"
    private const val KEY_SCREEN_OVERLAY_GUARD = "key_screen_overlay_guard"
  }
}
