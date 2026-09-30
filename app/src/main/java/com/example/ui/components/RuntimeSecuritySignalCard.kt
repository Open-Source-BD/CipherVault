package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AccessActionType
import com.example.security.AccessEvent
import com.example.security.AccessStatus
import com.example.security.RuntimeSecurityReport
import com.example.security.SignalLevel
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RuntimeSecuritySignalCard(
  report: RuntimeSecurityReport,
  accessLogs: List<AccessEvent>,
  onToggleExternalAccess: (Boolean) -> Unit,
  onRunScan: () -> Unit,
  onSimulateAccessAttempt: () -> Unit,
  onSimulateThreatToggle: (Boolean) -> Unit,
  onClearLogs: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showWarningDialog by remember { mutableStateOf(false) }
  var showLogsList by remember { mutableStateOf(false) }

  // Determine signal color & iconography
  val signalColor = when (report.signal) {
    SignalLevel.GREEN_SECURE -> SecurityGreen
    SignalLevel.YELLOW_WARNING -> SecurityAmber
    SignalLevel.RED_ALERT -> SecurityRed
  }

  val animatedSignalColor by animateColorAsState(
    targetValue = signalColor,
    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
    label = "signal_color_anim"
  )

  // Pulsing animation for active warnings / alerts
  val infiniteTransition = rememberInfiniteTransition(label = "signal_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (report.signal != SignalLevel.GREEN_SECURE) 1.25f else 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000),
      repeatMode = RepeatMode.Reverse
    ),
    label = "signal_pulse_scale"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("runtime_security_signal_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, animatedSignalColor.copy(alpha = 0.45f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      // Top Row: Signal Beacon + Status Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp)
          ) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(animatedSignalColor.copy(alpha = 0.25f))
            )
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(animatedSignalColor)
                .testTag("runtime_signal_beacon")
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = when (report.signal) {
                SignalLevel.GREEN_SECURE -> "SIGNAL: GREEN (PROTECTED)"
                SignalLevel.YELLOW_WARNING -> "SIGNAL: YELLOW (WARNING)"
                SignalLevel.RED_ALERT -> "SIGNAL: RED (CRITICAL ALERT)"
              },
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = animatedSignalColor
            )
            Text(
              text = if (report.signal == SignalLevel.GREEN_SECURE) "Runtime Isolated & Untampered" else "Access Alert / Monitoring Active",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }

        // Action: Run live scan
        OutlinedButton(
          onClick = onRunScan,
          modifier = Modifier.testTag("run_runtime_scan_btn"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Scan", style = MaterialTheme.typography.labelSmall)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Summary description box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(animatedSignalColor.copy(alpha = 0.10f))
          .padding(12.dp)
      ) {
        Row(verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = when (report.signal) {
              SignalLevel.GREEN_SECURE -> Icons.Default.CheckCircle
              SignalLevel.YELLOW_WARNING -> Icons.Default.Warning
              SignalLevel.RED_ALERT -> Icons.Default.Shield
            },
            contentDescription = null,
            tint = animatedSignalColor,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = report.headline,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = report.summary,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // User Access Control Master Switch
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "Allow External Apps to Access Data",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (report.isExternalAccessGranted) {
                "ENABLED: Third-party apps and background processes may request data (Signal: Yellow)"
              } else {
                "DISABLED: External access blocked. Vault is locked into isolated sandbox mode (Signal: Green)"
              },
              style = MaterialTheme.typography.bodySmall,
              color = if (report.isExternalAccessGranted) SecurityAmber else SecurityGreen
            )
          }

          Switch(
            checked = report.isExternalAccessGranted,
            onCheckedChange = { willEnable ->
              if (willEnable) {
                showWarningDialog = true
              } else {
                onToggleExternalAccess(false)
              }
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = SecurityAmber,
              checkedTrackColor = SecurityAmber.copy(alpha = 0.3f),
              uncheckedThumbColor = SecurityGreen,
              uncheckedTrackColor = SecurityGreen.copy(alpha = 0.3f)
            ),
            modifier = Modifier.testTag("toggle_external_access_switch")
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Verification & Simulation Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onSimulateAccessAttempt,
          modifier = Modifier
            .weight(1f)
            .testTag("simulate_external_access_btn"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Simulate App Probe", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = {
            val isThreatNow = report.signal == SignalLevel.RED_ALERT
            onSimulateThreatToggle(!isThreatNow)
          },
          modifier = Modifier
            .weight(1f)
            .testTag("simulate_debugger_threat_btn"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (report.signal == SignalLevel.RED_ALERT) "Clear Threat" else "Simulate Tracer",
            style = MaterialTheme.typography.labelSmall
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Access Event History Accordion Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable { showLogsList = !showLogsList }
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Access & Detection Log (${accessLogs.size})",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(8.dp))
          if (report.totalBlockedCount > 0) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SecurityRed.copy(alpha = 0.2f)
            ) {
              Text(
                text = "${report.totalBlockedCount} Blocked",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = SecurityRed,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Icon(
          imageVector = if (showLogsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Expandable Access Logs List
      AnimatedVisibility(visible = showLogsList) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          if (accessLogs.isEmpty()) {
            Text(
              text = "No access events recorded. System is quiet and clean.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.padding(vertical = 8.dp)
            )
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(onClick = onClearLogs) {
                Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Log", style = MaterialTheme.typography.labelSmall)
              }
            }

            val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

            accessLogs.take(10).forEach { event ->
              AccessEventRow(event = event, timeFormatted = dateFormat.format(Date(event.timestamp)))
              Spacer(modifier = Modifier.height(6.dp))
            }
          }
        }
      }
    }
  }

  // Confirmation dialog when enabling external app access
  if (showWarningDialog) {
    AlertDialog(
      onDismissRequest = { showWarningDialog = false },
      icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SecurityAmber) },
      title = { Text("Grant External App Access?") },
      text = {
        Text(
          "Enabling external access allows other apps, background sync services, and autofill providers to request credentials from this vault.\n\n" +
              "Your Security Signal will turn YELLOW (Warning) because external apps will have a communication bridge to this app. You can disable it again at any time."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onToggleExternalAccess(true)
            showWarningDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = SecurityAmber)
        ) {
          Text("Enable Access", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showWarningDialog = false }) {
          Text("Keep Blocked (Recommended)")
        }
      }
    )
  }
}

@Composable
private fun AccessEventRow(
  event: AccessEvent,
  timeFormatted: String
) {
  val (statusColor, badgeLabel) = when (event.status) {
    AccessStatus.BLOCKED -> Pair(SecurityRed, "BLOCKED")
    AccessStatus.ALLOWED -> Pair(SecurityGreen, "ALLOWED")
    AccessStatus.THREAT_DETECTED -> Pair(SecurityRed, "THREAT")
    AccessStatus.SUSPICIOUS -> Pair(SecurityAmber, "SUSPICIOUS")
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = RoundedCornerShape(5.dp),
            color = statusColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
          ) {
            Text(
              text = badgeLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
              ),
              color = statusColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = event.sourcePackage,
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
          )
        }

        Text(
          text = timeFormatted,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "${event.actionType.displayName} • ${event.details}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
      )
    }
  }
}
