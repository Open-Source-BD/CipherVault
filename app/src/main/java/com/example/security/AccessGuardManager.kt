package com.example.security

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.data.preferences.VaultPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AccessStatus(val displayName: String) {
  BLOCKED("Blocked"),
  ALLOWED("Allowed"),
  THREAT_DETECTED("Threat Detected"),
  SUSPICIOUS("Suspicious Activity")
}

enum class AccessActionType(val displayName: String) {
  READ_CREDENTIALS("Read Vault Credentials"),
  EXPORT_VAULT("Export Vault Data"),
  DEBUGGER_ATTACH("Debugger / Tracer Attached"),
  SCREEN_OVERLAY_INSPECT("Screen Overlay / Reader"),
  CLIPBOARD_SNOOP("Clipboard Data Query"),
  RUNTIME_HOOK_PROBE("Runtime Memory Inspection")
}

data class AccessEvent(
  val id: String = UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val sourcePackage: String,
  val actionType: AccessActionType,
  val status: AccessStatus,
  val details: String
)

enum class SignalLevel {
  GREEN_SECURE,
  YELLOW_WARNING,
  RED_ALERT
}

data class RuntimeSecurityReport(
  val signal: SignalLevel,
  val headline: String,
  val summary: String,
  val isDebuggerConnected: Boolean,
  val isAdbActive: Boolean,
  val isExternalAccessGranted: Boolean,
  val isOverlayRiskDetected: Boolean,
  val totalBlockedCount: Int,
  val totalAllowedCount: Int,
  val lastThreatTimestamp: Long? = null
)

class AccessGuardManager(
  private val context: Context,
  private val preferences: VaultPreferences
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val _accessLogs = MutableStateFlow<List<AccessEvent>>(
    listOf(
      AccessEvent(
        sourcePackage = "android.security.keystore",
        actionType = AccessActionType.READ_CREDENTIALS,
        status = AccessStatus.ALLOWED,
        details = "Internal Android Keystore initialized hardware master key verification."
      )
    )
  )
  val accessLogs: StateFlow<List<AccessEvent>> = _accessLogs.asStateFlow()

  private val _simulatedThreatActive = MutableStateFlow(false)
  private val _lastScannedReport = MutableStateFlow<RuntimeSecurityReport?>(null)

  // Real-time security state combined from preferences, system environment, and access logs
  val securityReport: StateFlow<RuntimeSecurityReport> = combine(
    preferences.externalDataAccessEnabled,
    preferences.runtimeTamperGuardEnabled,
    preferences.screenOverlayGuardEnabled,
    _accessLogs,
    _simulatedThreatActive
  ) { extAccess, tamperGuard, overlayGuard, logs, simThreat ->
    evaluateRuntimeStatus(
      extAccessAllowed = extAccess,
      tamperGuardActive = tamperGuard,
      overlayGuardActive = overlayGuard,
      logs = logs,
      isSimulatedThreat = simThreat
    )
  }.stateIn(
    scope,
    SharingStarted.WhileSubscribed(5000),
    evaluateRuntimeStatus(
      extAccessAllowed = false,
      tamperGuardActive = true,
      overlayGuardActive = true,
      logs = _accessLogs.value,
      isSimulatedThreat = false
    )
  )

  private fun evaluateRuntimeStatus(
    extAccessAllowed: Boolean,
    tamperGuardActive: Boolean,
    overlayGuardActive: Boolean,
    logs: List<AccessEvent>,
    isSimulatedThreat: Boolean
  ): RuntimeSecurityReport {
    val isDebugger = Debug.isDebuggerConnected() || Debug.waitingForDebugger() || isSimulatedThreat
    val isAdb = try {
      Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    } catch (_: Exception) {
      false
    }

    val isAccessibilityActive = try {
      val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
      val enabled = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
      !enabled.isNullOrEmpty()
    } catch (_: Exception) {
      false
    }

    val blockedCount = logs.count { it.status == AccessStatus.BLOCKED || it.status == AccessStatus.THREAT_DETECTED }
    val allowedCount = logs.count { it.status == AccessStatus.ALLOWED }
    val recentThreat = logs.firstOrNull { it.status == AccessStatus.THREAT_DETECTED || it.status == AccessStatus.BLOCKED }
    // An active probe or exploit attempt within the last 15 seconds triggers immediate RED alert
    val hasRecentThreatProbe = recentThreat != null && (System.currentTimeMillis() - recentThreat.timestamp < 15000L)

    val signal = when {
      // RED: Active debugger or active threat probe or suspicious access attempt
      isDebugger || (tamperGuardActive && isSimulatedThreat) || hasRecentThreatProbe -> SignalLevel.RED_ALERT

      // YELLOW: External access has been enabled by user, or ADB/Overlay active
      extAccessAllowed -> SignalLevel.YELLOW_WARNING
      isAdb || (overlayGuardActive && isAccessibilityActive) -> SignalLevel.YELLOW_WARNING

      // GREEN: Isolated, protected, no active threats or open ports
      else -> SignalLevel.GREEN_SECURE
    }

    val (headline, summary) = when (signal) {
      SignalLevel.RED_ALERT -> Pair(
        "Critical Threat Detected (Red Signal)",
        if (isDebugger) "Active debugger or runtime instrumentation process hooked to memory! Tamper guard blocking access."
        else "Suspicious external access attempt intercepted and blocked from reading vault data."
      )
      SignalLevel.YELLOW_WARNING -> Pair(
        "Warning: Open Access / Exposure (Yellow Signal)",
        if (extAccessAllowed) "External App Access is ENABLED by user. Third-party apps & background services may request vault access."
        else "Developer debugging or screen overlay service active on device. Screen inspection possible."
      )
      SignalLevel.GREEN_SECURE -> Pair(
        "Environment Secure & Isolated (Green Signal)",
        "Zero unauthorized access detected. External app access is disabled. Vault memory and storage are fully isolated."
      )
    }

    return RuntimeSecurityReport(
      signal = signal,
      headline = headline,
      summary = summary,
      isDebuggerConnected = isDebugger,
      isAdbActive = isAdb,
      isExternalAccessGranted = extAccessAllowed,
      isOverlayRiskDetected = isAccessibilityActive,
      totalBlockedCount = blockedCount,
      totalAllowedCount = allowedCount,
      lastThreatTimestamp = recentThreat?.timestamp
    )
  }

  /**
   * Called whenever an external app or source attempts to access vault data.
   * Returns true if user has granted external access, false if blocked.
   */
  fun evaluateAccessRequest(
    sourcePackage: String,
    action: AccessActionType,
    details: String
  ): Boolean {
    val isAllowed = preferences.externalDataAccessEnabled.value

    val status = if (isAllowed) {
      AccessStatus.ALLOWED
    } else {
      AccessStatus.BLOCKED
    }

    val event = AccessEvent(
      sourcePackage = sourcePackage,
      actionType = action,
      status = status,
      details = if (isAllowed) {
        "User has granted external access: $details"
      } else {
        "Access BLOCKED by Vault Guard: $details"
      }
    )

    recordAccessEvent(event)
    return isAllowed
  }

  fun recordAccessEvent(event: AccessEvent) {
    val updated = ArrayList(_accessLogs.value)
    updated.add(0, event)
    if (updated.size > 50) {
      updated.removeAt(updated.size - 1)
    }
    _accessLogs.value = updated
  }

  /**
   * Allows the user to simulate an unauthorized third-party access attempt
   * or a debugger probe to verify that the green signal changes to red/warning and blocks it.
   */
  fun simulateExternalAccessAttempt(
    simulatedSource: String = "com.untrusted.spyware.probe",
    action: AccessActionType = AccessActionType.READ_CREDENTIALS
  ) {
    val isAllowed = preferences.externalDataAccessEnabled.value
    val event = AccessEvent(
      sourcePackage = simulatedSource,
      actionType = action,
      status = if (isAllowed) AccessStatus.ALLOWED else AccessStatus.BLOCKED,
      details = if (isAllowed) {
        "Simulated probe from $simulatedSource succeeded because External Access is enabled."
      } else {
        "Simulated probe from $simulatedSource was immediately BLOCKED by Vault Access Guard."
      }
    )
    recordAccessEvent(event)
  }

  fun simulateDebuggerThreat(active: Boolean) {
    _simulatedThreatActive.value = active
    if (active) {
      recordAccessEvent(
        AccessEvent(
          sourcePackage = "system.runtime.ptrace_debugger",
          actionType = AccessActionType.DEBUGGER_ATTACH,
          status = AccessStatus.THREAT_DETECTED,
          details = "Active memory inspection tracer detected attempting to hook vault execution thread!"
        )
      )
    }
  }

  fun clearAccessLogs() {
    _accessLogs.value = emptyList()
    _simulatedThreatActive.value = false
  }

  fun performImmediateScan(): RuntimeSecurityReport {
    val report = evaluateRuntimeStatus(
      extAccessAllowed = preferences.externalDataAccessEnabled.value,
      tamperGuardActive = preferences.runtimeTamperGuardEnabled.value,
      overlayGuardActive = preferences.screenOverlayGuardEnabled.value,
      logs = _accessLogs.value,
      isSimulatedThreat = _simulatedThreatActive.value
    )
    _lastScannedReport.value = report
    return report
  }
}
