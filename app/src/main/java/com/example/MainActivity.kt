package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.security.AccessEvent
import com.example.security.BiometricHelper
import com.example.security.RuntimeSecurityReport
import com.example.ui.LockState
import com.example.ui.VaultViewModel
import com.example.ui.screens.AddEditItemDialog
import com.example.ui.screens.ExportBackupDialog
import com.example.ui.screens.ImportBackupDialog
import com.example.ui.screens.PasswordGeneratorScreen
import com.example.ui.screens.SecurityAuditScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultHomeScreen
import com.example.ui.screens.VaultLockScreen
import com.example.ui.theme.CipherVaultTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : FragmentActivity() {
  private val viewModel: VaultViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val screenProtection by viewModel.screenProtection.collectAsState()

      LaunchedEffect(screenProtection) {
        if (screenProtection) {
          window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
          )
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
      }

      CipherVaultTheme(themeMode = themeMode) {
        MainScreen(
          activity = this,
          viewModel = viewModel
        )
      }
    }
  }

  override fun onPause() {
    super.onPause()
    viewModel.checkAutoLock()
  }

  override fun onUserInteraction() {
    super.onUserInteraction()
    viewModel.notifyUserInteraction()
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
  activity: FragmentActivity,
  viewModel: VaultViewModel
) {
  val lockState by viewModel.lockState.collectAsState()
  val items by viewModel.filteredItems.collectAsState()
  val allVaultItems by viewModel.vaultItems.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val selectedCategory by viewModel.selectedCategory.collectAsState()
  val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
  val revealedItemIds by viewModel.revealedItemIds.collectAsState()
  val clipboardCountdown by viewModel.clipboardCountdown.collectAsState()
  val themeMode by viewModel.themeMode.collectAsState()
  val screenProtection by viewModel.screenProtection.collectAsState()
  val biometricEnabled by viewModel.biometricEnabled.collectAsState()
  val autoLockSeconds by viewModel.autoLockSeconds.collectAsState()
  val clipboardClearSeconds by viewModel.clipboardClearSeconds.collectAsState()
  val requireBiometricForReveal by viewModel.requireBiometricForReveal.collectAsState()
  val auditStats by viewModel.auditStats.collectAsState()
  val securityReport by viewModel.securityReport.collectAsState()
  val accessLogs by viewModel.accessLogs.collectAsState()
  val externalDataAccessEnabled by viewModel.externalDataAccessEnabled.collectAsState()
  val runtimeTamperGuardEnabled by viewModel.runtimeTamperGuardEnabled.collectAsState()
  val screenOverlayGuardEnabled by viewModel.screenOverlayGuardEnabled.collectAsState()

  val isBiometricAvailable = remember {
    BiometricHelper.isBiometricOrDeviceCredentialAvailable(activity)
  }

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  // Selected bottom navigation tab: 0=Vault, 1=Generator, 2=Audit, 3=Settings
  var currentTab by remember { mutableIntStateOf(0) }

  // Dialog states
  var editingItem by remember { mutableStateOf<VaultItem?>(null) }
  var isAddDialogOpen by remember { mutableStateOf(false) }
  var isExportDialogOpen by remember { mutableStateOf(false) }
  var isImportDialogOpen by remember { mutableStateOf(false) }

  // Temporary backup payload holder for SAF CreateDocument
  var pendingBackupPayload by remember { mutableStateOf<String?>(null) }
  var pendingImportContent by remember { mutableStateOf<String?>(null) }

  // SAF Launchers
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null && pendingBackupPayload != null) {
      scope.launch(Dispatchers.IO) {
        try {
          activity.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(pendingBackupPayload!!.toByteArray(Charsets.UTF_8))
          }
          viewModel.emitMessage("Backup exported to storage successfully")
        } catch (e: Exception) {
          viewModel.emitMessage("Failed to export backup: ${e.message}")
        } finally {
          pendingBackupPayload = null
        }
      }
    } else {
      pendingBackupPayload = null
    }
  }

  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      scope.launch(Dispatchers.IO) {
        try {
          val content = activity.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
          }
          if (content != null) {
            withContext(Dispatchers.Main) {
              pendingImportContent = content
              isImportDialogOpen = true
            }
          }
        } catch (e: Exception) {
          viewModel.emitMessage("Failed to read file: ${e.message}")
        }
      }
    }
  }

  // Collect toast messages for snackbar
  LaunchedEffect(Unit) {
    viewModel.toastMessage.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  // Biometric authentication helper function
  fun authenticateWithBiometrics(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit
  ) {
    BiometricHelper.authenticate(
      activity = activity,
      title = title,
      subtitle = subtitle,
      onSuccess = onSuccess,
      onError = { err ->
        viewModel.emitMessage("Authentication canceled or failed: $err")
      }
    )
  }

  if (lockState != LockState.UNLOCKED) {
    VaultLockScreen(
      lockState = lockState,
      isBiometricAvailable = isBiometricAvailable,
      isBiometricEnabled = biometricEnabled,
      onUnlockWithPassword = { password ->
        viewModel.unlockWithPassword(password)
      },
      onUnlockWithBiometric = {
        authenticateWithBiometrics(
          title = "Unlock CipherVault",
          subtitle = "Confirm fingerprint, face or PIN to decrypt vault",
          onSuccess = {
            viewModel.unlockWithBiometric()
          }
        )
      },
      onSetupMasterPassword = { password, enableBio ->
        viewModel.setupMasterPassword(password, enableBio)
      }
    )
  } else {
    val isImeVisible = WindowInsets.isImeVisible

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        AnimatedVisibility(
          visible = !isImeVisible,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
          ) {
            NavigationBarItem(
              selected = currentTab == 0,
              onClick = { currentTab = 0 },
              icon = { Icon(Icons.Default.Key, contentDescription = "Vault") },
              label = { Text("Vault", fontWeight = FontWeight.SemiBold) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
              ),
              modifier = Modifier.testTag("nav_tab_vault")
            )
            NavigationBarItem(
              selected = currentTab == 1,
              onClick = { currentTab = 1 },
              icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generator") },
              label = { Text("Generator", fontWeight = FontWeight.SemiBold) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
              ),
              modifier = Modifier.testTag("nav_tab_generator")
            )
            NavigationBarItem(
              selected = currentTab == 2,
              onClick = { currentTab = 2 },
              icon = { Icon(Icons.Default.Security, contentDescription = "Audit") },
              label = { Text("Audit", fontWeight = FontWeight.SemiBold) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
              ),
              modifier = Modifier.testTag("nav_tab_audit")
            )
            NavigationBarItem(
              selected = currentTab == 3,
              onClick = { currentTab = 3 },
              icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
              label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
              ),
              modifier = Modifier.testTag("nav_tab_settings")
            )
          }
        }
      }
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .imePadding()
      ) {
        when (currentTab) {
          0 -> {
            VaultHomeScreen(
              items = items,
              searchQuery = searchQuery,
              selectedCategory = selectedCategory,
              showFavoritesOnly = showFavoritesOnly,
              revealedItemIds = revealedItemIds,
              clipboardCountdown = clipboardCountdown,
              securityReport = securityReport,
              accessLogs = accessLogs,
              onToggleExternalAccess = { viewModel.setExternalDataAccessEnabled(it) },
              onRunRuntimeScan = { viewModel.runRuntimeSecurityScan() },
              onSimulateAccessAttempt = { viewModel.simulateExternalAccessAttempt() },
              onSimulateThreatToggle = { viewModel.simulateThreatToggle(it) },
              onClearAccessLogs = { viewModel.clearAccessLogs() },
              onSearchChange = { viewModel.searchQuery.value = it },
              onCategorySelect = { viewModel.selectedCategory.value = it },
              onToggleFavoritesOnly = {
                viewModel.showFavoritesOnly.value = !viewModel.showFavoritesOnly.value
              },
              onRequestPasswordReveal = { item ->
                if (requireBiometricForReveal && isBiometricAvailable) {
                  authenticateWithBiometrics(
                    title = "Reveal Password",
                    subtitle = "Verify identity to view password for ${item.title}",
                    onSuccess = {
                      viewModel.onBiometricVerifiedForReveal(item.id)
                    }
                  )
                } else {
                  viewModel.onBiometricVerifiedForReveal(item.id)
                }
              },
              onHidePassword = { itemId ->
                viewModel.hidePassword(itemId)
              },
              onRequestPasswordCopy = { item ->
                if (requireBiometricForReveal && isBiometricAvailable) {
                  authenticateWithBiometrics(
                    title = "Copy Password",
                    subtitle = "Verify identity to copy password for ${item.title}",
                    onSuccess = {
                      viewModel.copyPassword(item.password)
                    }
                  )
                } else {
                  viewModel.copyPassword(item.password)
                }
              },
              onCopyUsername = { username ->
                viewModel.copyUsername(username)
              },
              onToggleFavorite = { item ->
                viewModel.toggleFavorite(item)
              },
              onEditItem = { item ->
                editingItem = item
              },
              onDeleteItem = { item ->
                viewModel.deleteItem(item)
              },
              onAddNewItem = {
                isAddDialogOpen = true
              },
              onLockVault = {
                viewModel.lockVault()
              },
              onClearClipboard = {
                viewModel.clipboardHelper.clearClipboard()
              }
            )
          }

          1 -> {
            PasswordGeneratorScreen(
              onCopyPassword = { generated ->
                viewModel.copyPassword(generated)
              },
              onSaveToVault = { generated ->
                editingItem = VaultItem(
                  title = "",
                  password = generated
                )
              }
            )
          }

          2 -> {
            SecurityAuditScreen(
              auditStats = auditStats,
              vaultItems = allVaultItems,
              securityReport = securityReport,
              accessLogs = accessLogs,
              onToggleExternalAccess = { viewModel.setExternalDataAccessEnabled(it) },
              onRunRuntimeScan = { viewModel.runRuntimeSecurityScan() },
              onSimulateAccessAttempt = { viewModel.simulateExternalAccessAttempt() },
              onSimulateThreatToggle = { viewModel.simulateThreatToggle(it) },
              onClearAccessLogs = { viewModel.clearAccessLogs() },
              onEditItem = { item ->
                editingItem = item
              }
            )
          }

          3 -> {
            SettingsScreen(
              themeMode = themeMode,
              screenProtection = screenProtection,
              biometricEnabled = biometricEnabled,
              isBiometricAvailable = isBiometricAvailable,
              autoLockSeconds = autoLockSeconds,
              clipboardClearSeconds = clipboardClearSeconds,
              requireBiometricForReveal = requireBiometricForReveal,
              securityReport = securityReport,
              accessLogs = accessLogs,
              externalDataAccessEnabled = externalDataAccessEnabled,
              runtimeTamperGuardEnabled = runtimeTamperGuardEnabled,
              screenOverlayGuardEnabled = screenOverlayGuardEnabled,
              onThemeModeChange = { viewModel.setThemeMode(it) },
              onScreenProtectionChange = { viewModel.setScreenProtection(it) },
              onBiometricToggle = { enable ->
                if (enable) {
                  authenticateWithBiometrics(
                    title = "Enable Biometric Access",
                    subtitle = "Confirm fingerprint, face or PIN to secure vault keys",
                    onSuccess = {
                      viewModel.enableBiometricWithCurrentKey()
                    }
                  )
                } else {
                  viewModel.disableBiometric()
                }
              },
              onRequireBiometricForRevealChange = { viewModel.setRequireBiometricForReveal(it) },
              onAutoLockChange = { viewModel.setAutoLockSeconds(it) },
              onClipboardClearChange = { viewModel.setClipboardClearSeconds(it) },
              onToggleExternalAccess = { viewModel.setExternalDataAccessEnabled(it) },
              onToggleRuntimeTamperGuard = { viewModel.setRuntimeTamperGuardEnabled(it) },
              onToggleScreenOverlayGuard = { viewModel.setScreenOverlayGuardEnabled(it) },
              onRunRuntimeScan = { viewModel.runRuntimeSecurityScan() },
              onSimulateAccessAttempt = { viewModel.simulateExternalAccessAttempt() },
              onSimulateThreatToggle = { viewModel.simulateThreatToggle(it) },
              onClearAccessLogs = { viewModel.clearAccessLogs() },
              onExportBackup = {
                isExportDialogOpen = true
              },
              onImportBackup = {
                openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
              },
              onLockVaultNow = {
                viewModel.lockVault()
              }
            )
          }
        }
      }
    }
  }

  // Add Item Dialog
  if (isAddDialogOpen) {
    AddEditItemDialog(
      initialItem = null,
      onDismiss = { isAddDialogOpen = false },
      onSave = { newItem ->
        viewModel.saveItem(newItem)
      }
    )
  }

  // Edit Item Dialog
  editingItem?.let { item ->
    AddEditItemDialog(
      initialItem = item,
      onDismiss = { editingItem = null },
      onSave = { updated ->
        viewModel.saveItem(updated)
        editingItem = null
      }
    )
  }

  // Export Backup Dialog
  if (isExportDialogOpen) {
    ExportBackupDialog(
      onDismiss = { isExportDialogOpen = false },
      onConfirm = { passphrase ->
        isExportDialogOpen = false
        val encryptedJson = viewModel.exportEncryptedBackup(passphrase)
        pendingBackupPayload = encryptedJson
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        createDocumentLauncher.launch("CipherVault_backup_$dateStr.ciphervault")
      }
    )
  }

  // Import Backup Dialog
  if (isImportDialogOpen && pendingImportContent != null) {
    ImportBackupDialog(
      onDismiss = {
        isImportDialogOpen = false
        pendingImportContent = null
      },
      onConfirm = { passphrase ->
        val content = pendingImportContent!!
        isImportDialogOpen = false
        pendingImportContent = null
        viewModel.importEncryptedBackup(
          content = content,
          passphrase = passphrase,
          onSuccess = { count ->
            // Success handled in ViewModel
          },
          onError = { error ->
            viewModel.emitMessage(error)
          }
        )
      }
    )
  }
}
