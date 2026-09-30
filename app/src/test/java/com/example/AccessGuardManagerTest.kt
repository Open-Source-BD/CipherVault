package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.VaultPreferences
import com.example.security.AccessActionType
import com.example.security.AccessGuardManager
import com.example.security.AccessStatus
import com.example.security.SignalLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccessGuardManagerTest {

  private lateinit var preferences: VaultPreferences
  private lateinit var guardManager: AccessGuardManager

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    preferences = VaultPreferences(context)
    preferences.setExternalDataAccessEnabled(false)
    preferences.setRuntimeTamperGuardEnabled(true)
    preferences.setScreenOverlayGuardEnabled(true)
    guardManager = AccessGuardManager(context, preferences)
    guardManager.clearAccessLogs()
  }

  @Test
  fun testInitialReportIsIsolatedAndProtected() {
    val report = guardManager.performImmediateScan()
    assertNotNull(report)
    assertFalse("Default should block external data access", report.isExternalAccessGranted)
    assertEquals("Should start in GREEN signal state when isolated and safe", SignalLevel.GREEN_SECURE, report.signal)
  }

  @Test
  fun testEnablingExternalAccessTransitionsToYellowSignal() {
    preferences.setExternalDataAccessEnabled(true)
    val report = guardManager.performImmediateScan()
    assertTrue("External data access should be enabled", report.isExternalAccessGranted)
    assertEquals("Open access mode should emit a YELLOW warning signal", SignalLevel.YELLOW_WARNING, report.signal)
  }

  @Test
  fun testUnauthorizedAccessAttemptLoggedAndAlerts() {
    // When external data access is disabled
    preferences.setExternalDataAccessEnabled(false)

    val allowed = guardManager.evaluateAccessRequest(
      sourcePackage = "com.unauthorized.thirdparty.spy",
      action = AccessActionType.READ_CREDENTIALS,
      details = "Direct credential scrape query"
    )

    assertFalse("Unauthorized external caller must be blocked", allowed)

    val report = guardManager.performImmediateScan()
    assertEquals("Blocked probe must trigger active threat (RED signal)", SignalLevel.RED_ALERT, report.signal)

    val logs = guardManager.accessLogs.value
    assertTrue("Probe event should be logged", logs.any { it.sourcePackage == "com.unauthorized.thirdparty.spy" })
    val loggedEvent = logs.first { it.sourcePackage == "com.unauthorized.thirdparty.spy" }
    assertEquals("Log must state the attempt was blocked", AccessStatus.BLOCKED, loggedEvent.status)
  }

  @Test
  fun testAuthorizedAccessWhenUserEnablesAccess() {
    // User explicitly grants external access
    preferences.setExternalDataAccessEnabled(true)

    val allowed = guardManager.evaluateAccessRequest(
      sourcePackage = "com.partner.autofill.service",
      action = AccessActionType.READ_CREDENTIALS,
      details = "Autofill credential fill request"
    )

    assertTrue("Access should be allowed when user enabled it", allowed)

    val logs = guardManager.accessLogs.value
    val event = logs.firstOrNull { it.sourcePackage == "com.partner.autofill.service" }
    assertNotNull(event)
    assertEquals("Log must record granted access", AccessStatus.ALLOWED, event?.status)
  }

  @Test
  fun testSimulatedThreatTriggersRedSignal() {
    guardManager.simulateDebuggerThreat(true)
    val report = guardManager.performImmediateScan()
    assertEquals("Simulated threat must cause RED warning signal", SignalLevel.RED_ALERT, report.signal)

    guardManager.simulateDebuggerThreat(false)
    guardManager.clearAccessLogs()
    val restoredReport = guardManager.performImmediateScan()
    // Cleared threat and logs returns to green
    assertEquals("Restoring safe runtime should return to GREEN signal", SignalLevel.GREEN_SECURE, restoredReport.signal)
  }
}

