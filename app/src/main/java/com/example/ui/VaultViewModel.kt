package com.example.ui

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoManager
import com.example.crypto.PasswordStrengthInfo
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.data.preferences.VaultPreferences
import com.example.data.repository.VaultRepository
import com.example.security.AccessEvent
import com.example.security.AccessGuardManager
import com.example.security.BiometricHelper
import com.example.security.ClipboardHelper
import com.example.security.RuntimeSecurityReport
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

enum class LockState {
  NOT_INITIALIZED,
  LOCKED,
  UNLOCKED
}

data class AuditStats(
  val totalItems: Int = 0,
  val weakPasswords: Int = 0,
  val reusedPasswords: Int = 0,
  val strongPasswords: Int = 0,
  val securityScore: Int = 100 // 0-100
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {
  private val preferences = VaultPreferences(application)
  private val database = AppDatabase.getDatabase(application)
  private val repository = VaultRepository(database.vaultDao())
  val clipboardHelper = ClipboardHelper(application)
  val accessGuardManager = AccessGuardManager(application, preferences)

  val securityReport: StateFlow<RuntimeSecurityReport> = accessGuardManager.securityReport
  val accessLogs: StateFlow<List<AccessEvent>> = accessGuardManager.accessLogs

  private val _lockState = MutableStateFlow(
    if (!preferences.isVaultInitialized()) LockState.NOT_INITIALIZED else LockState.LOCKED
  )
  val lockState: StateFlow<LockState> = _lockState.asStateFlow()

  private var activeMasterKey: SecretKey? = null

  // Vault Items
  private val _vaultItems = MutableStateFlow<List<VaultItem>>(emptyList())
  val vaultItems: StateFlow<List<VaultItem>> = _vaultItems.asStateFlow()

  // Search & Filters
  val searchQuery = MutableStateFlow("")
  val selectedCategory = MutableStateFlow(VaultCategory.ALL)
  val showFavoritesOnly = MutableStateFlow(false)

  // Revealed password IDs (per-session authenticated reveal)
  private val _revealedItemIds = MutableStateFlow<Set<Long>>(emptySet())
  val revealedItemIds: StateFlow<Set<Long>> = _revealedItemIds.asStateFlow()

  // Filtered Items
  val filteredItems: StateFlow<List<VaultItem>> = combine(
    _vaultItems,
    searchQuery,
    selectedCategory,
    showFavoritesOnly
  ) { items, query, category, favOnly ->
    items.filter { item ->
      val matchesQuery = query.isBlank() ||
          item.title.contains(query, ignoreCase = true) ||
          item.username.contains(query, ignoreCase = true) ||
          item.website.contains(query, ignoreCase = true) ||
          item.notes.contains(query, ignoreCase = true)

      val matchesCategory = category == VaultCategory.ALL || item.category == category
      val matchesFav = !favOnly || item.isFavorite

      matchesQuery && matchesCategory && matchesFav
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Security Audit
  val auditStats: StateFlow<AuditStats> = _vaultItems.combine(_vaultItems) { items, _ ->
    calculateAudit(items)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuditStats())

  // Settings
  val themeMode = preferences.themeMode
  val screenProtection = preferences.screenProtection
  val biometricEnabled = preferences.biometricEnabled
  val autoLockSeconds = preferences.autoLockSeconds
  val clipboardClearSeconds = preferences.clipboardClearSeconds
  val requireBiometricForReveal = preferences.requireBiometricForReveal
  val externalDataAccessEnabled = preferences.externalDataAccessEnabled
  val runtimeTamperGuardEnabled = preferences.runtimeTamperGuardEnabled
  val screenOverlayGuardEnabled = preferences.screenOverlayGuardEnabled
  val clipboardCountdown = clipboardHelper.clipboardCountdown

  // Snackbar notifications
  private val _toastMessage = MutableSharedFlow<String>()
  val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

  private var lastActiveTimestamp: Long = System.currentTimeMillis()

  init {
    // If biometric is enabled and available, user can unlock via biometrics immediately
  }

  fun notifyUserInteraction() {
    lastActiveTimestamp = System.currentTimeMillis()
  }

  fun checkAutoLock() {
    if (_lockState.value != LockState.UNLOCKED) return
    val timeout = preferences.autoLockSeconds.value
    if (timeout == 0) {
      lockVault()
      return
    }
    val elapsed = (System.currentTimeMillis() - lastActiveTimestamp) / 1000
    if (elapsed >= timeout) {
      lockVault()
    }
  }

  fun setupMasterPassword(password: String, enableBiometric: Boolean = false): Boolean {
    if (password.length < 6) return false
    val salt = CryptoManager.generateSalt()
    val key = CryptoManager.deriveKey(password.toCharArray(), salt)
    val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

    // Create verification token
    val (verifierCipher, iv) = CryptoManager.encrypt(VERIFICATION_MAGIC, key)
    preferences.saveMasterVerifier(saltBase64, verifierCipher, iv)

    activeMasterKey = key

    if (enableBiometric) {
      try {
        val (encKey, bioIv) = BiometricHelper.encryptMasterKeyWithKeyStore(key.encoded)
        preferences.saveBiometricEncryptedKey(encKey, bioIv)
      } catch (_: Exception) {
        // Biometric keystore failed, continue with password
      }
    }

    _lockState.value = LockState.UNLOCKED
    startCollectingItems(key)
    emitMessage("Vault created successfully with AES-256-GCM encryption")
    return true
  }

  fun unlockWithPassword(password: String): Boolean {
    val saltBase64 = preferences.getMasterSalt() ?: return false
    val verifierCipher = preferences.getVerifierCipher() ?: return false
    val verifierIv = preferences.getVerifierIv() ?: return false

    return try {
      val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
      val key = CryptoManager.deriveKey(password.toCharArray(), salt)
      val decrypted = CryptoManager.decrypt(verifierCipher, verifierIv, key)

      if (decrypted == VERIFICATION_MAGIC) {
        activeMasterKey = key
        _lockState.value = LockState.UNLOCKED
        startCollectingItems(key)
        emitMessage("Vault unlocked")
        true
      } else {
        false
      }
    } catch (_: Exception) {
      false
    }
  }

  fun unlockWithBiometric(): Boolean {
    val bioData = preferences.getBiometricEncryptedKey() ?: return false
    return try {
      val key = BiometricHelper.decryptMasterKeyWithKeyStore(bioData.first, bioData.second)
      val verifierCipher = preferences.getVerifierCipher() ?: return false
      val verifierIv = preferences.getVerifierIv() ?: return false

      val decrypted = CryptoManager.decrypt(verifierCipher, verifierIv, key)
      if (decrypted == VERIFICATION_MAGIC) {
        activeMasterKey = key
        _lockState.value = LockState.UNLOCKED
        startCollectingItems(key)
        emitMessage("Vault unlocked with Biometrics")
        true
      } else {
        false
      }
    } catch (_: Exception) {
      false
    }
  }

  fun enableBiometricWithCurrentKey(): Boolean {
    val key = activeMasterKey ?: return false
    return try {
      val (encKey, bioIv) = BiometricHelper.encryptMasterKeyWithKeyStore(key.encoded)
      preferences.saveBiometricEncryptedKey(encKey, bioIv)
      emitMessage("Biometric unlock enabled")
      true
    } catch (e: Exception) {
      emitMessage("Failed to enable biometrics: ${e.message}")
      false
    }
  }

  fun disableBiometric() {
    preferences.disableBiometric()
    emitMessage("Biometric unlock disabled")
  }

  fun lockVault() {
    activeMasterKey = null
    _revealedItemIds.value = emptySet()
    _vaultItems.value = emptyList()
    clipboardHelper.clearClipboard()
    _lockState.value = if (!preferences.isVaultInitialized()) LockState.NOT_INITIALIZED else LockState.LOCKED
    emitMessage("Vault locked")
  }

  private fun startCollectingItems(key: SecretKey) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.getDecryptedItems(key).collect { items ->
        _vaultItems.value = items
      }
    }
  }

  fun saveItem(item: VaultItem) {
    val key = activeMasterKey ?: return
    viewModelScope.launch(Dispatchers.IO) {
      if (item.id == 0L) {
        repository.insertItem(item, key)
        emitMessage("Added ${item.title}")
      } else {
        repository.updateItem(item, key)
        emitMessage("Updated ${item.title}")
      }
    }
  }

  fun deleteItem(item: VaultItem) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.deleteItem(item.id)
      emitMessage("Deleted ${item.title}")
    }
  }

  fun toggleFavorite(item: VaultItem) {
    val key = activeMasterKey ?: return
    viewModelScope.launch(Dispatchers.IO) {
      repository.updateItem(item.copy(isFavorite = !item.isFavorite), key)
    }
  }

  fun onBiometricVerifiedForReveal(itemId: Long) {
    _revealedItemIds.value = _revealedItemIds.value + itemId
  }

  fun hidePassword(itemId: Long) {
    _revealedItemIds.value = _revealedItemIds.value - itemId
  }

  fun isPasswordRevealed(itemId: Long): Boolean {
    return _revealedItemIds.value.contains(itemId)
  }

  fun copyPassword(password: String) {
    clipboardHelper.copySensitive(
      text = password,
      label = "Password",
      timeoutSeconds = preferences.clipboardClearSeconds.value
    )
    emitMessage("Password copied! Clipboard auto-clears in ${preferences.clipboardClearSeconds.value}s")
  }

  fun copyUsername(username: String) {
    clipboardHelper.copySensitive(
      text = username,
      label = "Username",
      timeoutSeconds = preferences.clipboardClearSeconds.value
    )
    emitMessage("Username copied!")
  }

  fun exportEncryptedBackup(passphrase: String): String {
    val items = _vaultItems.value
    return BackupManager.exportEncryptedBackup(items, passphrase)
  }

  fun importEncryptedBackup(
    content: String,
    passphrase: String,
    onSuccess: (Int) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val key = activeMasterKey
      if (key == null) {
        onError("Vault must be unlocked to import backup")
        return@launch
      }

      val result = BackupManager.importEncryptedBackup(content, passphrase)
      result.onSuccess { importedItems ->
        importedItems.forEach { item ->
          repository.insertItem(item, key)
        }
        emitMessage("Successfully restored ${importedItems.size} items")
        onSuccess(importedItems.size)
      }.onFailure { error ->
        onError(error.message ?: "Failed to import backup")
      }
    }
  }

  fun setThemeMode(mode: AppThemeMode) {
    preferences.setThemeMode(mode)
  }

  fun setScreenProtection(enabled: Boolean) {
    preferences.setScreenProtection(enabled)
  }

  fun setAutoLockSeconds(seconds: Int) {
    preferences.setAutoLockSeconds(seconds)
  }

  fun setClipboardClearSeconds(seconds: Int) {
    preferences.setClipboardClearSeconds(seconds)
  }

  fun setRequireBiometricForReveal(required: Boolean) {
    preferences.setRequireBiometricForReveal(required)
  }

  fun setExternalDataAccessEnabled(enabled: Boolean) {
    preferences.setExternalDataAccessEnabled(enabled)
    if (enabled) {
      emitMessage("Warning: External app data access is now ENABLED (Signal: Yellow)")
    } else {
      emitMessage("External app data access BLOCKED. Vault sandbox is isolated (Signal: Green)")
    }
  }

  fun setRuntimeTamperGuardEnabled(enabled: Boolean) {
    preferences.setRuntimeTamperGuardEnabled(enabled)
  }

  fun setScreenOverlayGuardEnabled(enabled: Boolean) {
    preferences.setScreenOverlayGuardEnabled(enabled)
  }

  fun runRuntimeSecurityScan() {
    val report = accessGuardManager.performImmediateScan()
    emitMessage("Runtime Scan Complete: ${report.headline}")
  }

  fun simulateExternalAccessAttempt() {
    accessGuardManager.simulateExternalAccessAttempt()
    val isAllowed = preferences.externalDataAccessEnabled.value
    if (isAllowed) {
      emitMessage("Simulated probe ALLOWED (External access is ON)")
    } else {
      emitMessage("Simulated probe BLOCKED! Access attempt intercepted.")
    }
  }

  fun simulateThreatToggle(active: Boolean) {
    accessGuardManager.simulateDebuggerThreat(active)
    if (active) {
      emitMessage("ALERT: Simulated runtime tracer attached (Signal: Red)")
    } else {
      emitMessage("Simulated runtime tracer cleared")
    }
  }

  fun clearAccessLogs() {
    accessGuardManager.clearAccessLogs()
    emitMessage("Access & audit logs cleared")
  }

  private fun calculateAudit(items: List<VaultItem>): AuditStats {
    if (items.isEmpty()) return AuditStats()

    var weak = 0
    var strong = 0
    val passCountMap = mutableMapOf<String, Int>()

    items.forEach { item ->
      if (item.password.isNotEmpty()) {
        passCountMap[item.password] = (passCountMap[item.password] ?: 0) + 1
        val strength = CryptoManager.calculatePasswordStrength(item.password)
        if (strength.score <= 2) weak++
        if (strength.score >= 4) strong++
      }
    }

    val reused = passCountMap.values.count { it > 1 }

    // Security score calculation
    val penaltyWeak = (weak.toDouble() / items.size) * 40
    val penaltyReused = (reused.toDouble() / items.size) * 35
    val bonusStrong = (strong.toDouble() / items.size) * 25
    val score = (100 - penaltyWeak - penaltyReused + (bonusStrong - 25)).toInt().coerceIn(10, 100)

    return AuditStats(
      totalItems = items.size,
      weakPasswords = weak,
      reusedPasswords = reused,
      strongPasswords = strong,
      securityScore = score
    )
  }

  fun emitMessage(message: String) {
    viewModelScope.launch {
      _toastMessage.emit(message)
    }
  }

  companion object {
    private const val VERIFICATION_MAGIC = "CIPHERVAULT_VERIFIED_TOKEN_2026"
  }
}
