package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AccessEvent
import com.example.security.RuntimeSecurityReport
import com.example.ui.components.RuntimeSecuritySignalCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityRed

@Composable
fun SettingsScreen(
  themeMode: AppThemeMode,
  screenProtection: Boolean,
  biometricEnabled: Boolean,
  isBiometricAvailable: Boolean,
  autoLockSeconds: Int,
  clipboardClearSeconds: Int,
  requireBiometricForReveal: Boolean,
  securityReport: RuntimeSecurityReport,
  accessLogs: List<AccessEvent>,
  externalDataAccessEnabled: Boolean,
  runtimeTamperGuardEnabled: Boolean,
  screenOverlayGuardEnabled: Boolean,
  onThemeModeChange: (AppThemeMode) -> Unit,
  onScreenProtectionChange: (Boolean) -> Unit,
  onBiometricToggle: (Boolean) -> Unit,
  onRequireBiometricForRevealChange: (Boolean) -> Unit,
  onAutoLockChange: (Int) -> Unit,
  onClipboardClearChange: (Int) -> Unit,
  onToggleExternalAccess: (Boolean) -> Unit,
  onToggleRuntimeTamperGuard: (Boolean) -> Unit,
  onToggleScreenOverlayGuard: (Boolean) -> Unit,
  onRunRuntimeScan: () -> Unit,
  onSimulateAccessAttempt: () -> Unit,
  onSimulateThreatToggle: (Boolean) -> Unit,
  onClearAccessLogs: () -> Unit,
  onExportBackup: () -> Unit,
  onImportBackup: () -> Unit,
  onLockVaultNow: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 20.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Vault Settings",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = "Security preferences, zero-knowledge policies & backups",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )

    // Section 0: Real-time Runtime Security Signal & App Access Guard
    RuntimeSecuritySignalCard(
      report = securityReport,
      accessLogs = accessLogs,
      onToggleExternalAccess = onToggleExternalAccess,
      onRunScan = onRunRuntimeScan,
      onSimulateAccessAttempt = onSimulateAccessAttempt,
      onSimulateThreatToggle = onSimulateThreatToggle,
      onClearLogs = onClearAccessLogs
    )

    // Section 1: Appearance & Dark Theme
    SettingsSectionCard(title = "Appearance & Dark Theme", icon = Icons.Default.Palette) {
      Text(
        text = "Choose your preferred theme. Pure AMOLED Black is optimized for ultra-low light and battery saving.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(
          Pair(AppThemeMode.AMOLED_BLACK, "AMOLED"),
          Pair(AppThemeMode.DARK_SLATE, "Deep Slate"),
          Pair(AppThemeMode.SYSTEM, "System"),
          Pair(AppThemeMode.LIGHT, "Light")
        ).forEach { (mode, label) ->
          FilterChip(
            selected = themeMode == mode,
            onClick = { onThemeModeChange(mode) },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("theme_chip_${mode.name}")
          )
        }
      }
    }

    // Section 2: Biometric & Master Access
    SettingsSectionCard(title = "Biometrics & Access Control", icon = Icons.Default.Fingerprint) {
      SettingToggleRow(
        title = "Biometric Vault Unlock",
        subtitle = if (isBiometricAvailable) "Use fingerprint, facial scan, or device PIN to decrypt" else "Biometrics not enrolled on device",
        checked = biometricEnabled && isBiometricAvailable,
        enabled = isBiometricAvailable,
        onCheckedChange = onBiometricToggle,
        testTag = "toggle_biometric_unlock"
      )

      SettingToggleRow(
        title = "Biometric Reveal Check",
        subtitle = "Authenticate every time before showing or copying any password",
        checked = requireBiometricForReveal,
        enabled = true,
        onCheckedChange = onRequireBiometricForRevealChange,
        testTag = "toggle_require_biometric_reveal"
      )

      SettingToggleRow(
        title = "Screen Protection (FLAG_SECURE)",
        subtitle = "Blocks screenshots & hides app in task switcher (blacks out screen recordings and remote streaming previews)",
        checked = screenProtection,
        enabled = true,
        onCheckedChange = onScreenProtectionChange,
        testTag = "toggle_screen_protection"
      )

      SettingToggleRow(
        title = "Runtime Tamper & Tracer Guard",
        subtitle = "Detects debuggers, ptrace tracing, and suspicious memory hooks (alerts in Red Signal)",
        checked = runtimeTamperGuardEnabled,
        enabled = true,
        onCheckedChange = onToggleRuntimeTamperGuard,
        testTag = "toggle_runtime_tamper_guard"
      )

      SettingToggleRow(
        title = "Screen Overlay & Reader Guard",
        subtitle = "Detects floating system overlays or screen inspectors attempting to view vault content",
        checked = screenOverlayGuardEnabled,
        enabled = true,
        onCheckedChange = onToggleScreenOverlayGuard,
        testTag = "toggle_screen_overlay_guard"
      )
    }

    // Section 3: Timeouts & Auto-Clearing
    SettingsSectionCard(title = "Security Timers", icon = Icons.Default.Timer) {
      Text(
        text = "Auto-Lock Vault After Inactivity",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(
          Pair(0, "Immediate"),
          Pair(30, "30s"),
          Pair(60, "1 min"),
          Pair(300, "5 min")
        ).forEach { (seconds, label) ->
          FilterChip(
            selected = autoLockSeconds == seconds,
            onClick = { onAutoLockChange(seconds) },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Clipboard Auto-Clear Timeout",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(
          Pair(15, "15s"),
          Pair(30, "30s"),
          Pair(60, "60s")
        ).forEach { (seconds, label) ->
          FilterChip(
            selected = clipboardClearSeconds == seconds,
            onClick = { onClipboardClearChange(seconds) },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
          )
        }
      }
    }

    // Section 4: Encrypted Backups
    SettingsSectionCard(title = "Encrypted Backups & Redundancy", icon = Icons.Default.Download) {
      Text(
        text = "Export AES-256-GCM encrypted backup files to external storage. Backups are self-contained and password-protected.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onExportBackup,
          modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .testTag("export_backup_btn"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export Backup", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = onImportBackup,
          modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .testTag("import_backup_btn"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Restore Backup", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    // Section 5: Lock Action
    Button(
      onClick = onLockVaultNow,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("lock_vault_now_btn"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onError
      )
    ) {
      Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Lock Vault Now", fontWeight = FontWeight.Bold)
    }

    // Section 6: Architecture & Transparency
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      )
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Offline & Open Source Verification",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "• Zero Cloud: AndroidManifest does not request android.permission.INTERNET.\n" +
              "• Encrypted at rest: Room database only stores AES-GCM ciphertext.\n" +
              "• Android KeyStore hardware root of trust.\n" +
              "• Exported backups are protected by SHA-256 checksum and PBKDF2.",
          style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: @Composable () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      enabled = enabled,
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
      ),
      modifier = Modifier.testTag(testTag)
    )
  }
}
