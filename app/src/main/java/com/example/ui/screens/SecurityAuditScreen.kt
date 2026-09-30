package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager
import com.example.data.model.VaultItem
import com.example.security.AccessEvent
import com.example.security.RuntimeSecurityReport
import com.example.ui.AuditStats
import com.example.ui.components.RuntimeSecuritySignalCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityRed

@Composable
fun SecurityAuditScreen(
  auditStats: AuditStats,
  vaultItems: List<VaultItem>,
  securityReport: RuntimeSecurityReport,
  accessLogs: List<AccessEvent>,
  onToggleExternalAccess: (Boolean) -> Unit,
  onRunRuntimeScan: () -> Unit,
  onSimulateAccessAttempt: () -> Unit,
  onSimulateThreatToggle: (Boolean) -> Unit,
  onClearAccessLogs: () -> Unit,
  onEditItem: (VaultItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val weakItems = remember(vaultItems) {
    vaultItems.filter {
      it.password.isNotEmpty() && CryptoManager.calculatePasswordStrength(it.password).score <= 2
    }
  }

  val passwordCountMap = remember(vaultItems) {
    val map = mutableMapOf<String, Int>()
    vaultItems.forEach {
      if (it.password.isNotEmpty()) {
        map[it.password] = (map[it.password] ?: 0) + 1
      }
    }
    map
  }

  val reusedItems = remember(vaultItems, passwordCountMap) {
    vaultItems.filter {
      it.password.isNotEmpty() && (passwordCountMap[it.password] ?: 0) > 1
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Security Audit",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "Local offline cryptographic hygiene and vulnerability assessment",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
      )
    }

    // Health Score Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Vault Health Score",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = when {
                auditStats.securityScore >= 85 -> "Excellent security posture. Low risk."
                auditStats.securityScore >= 60 -> "Moderate risk. Fix reused or weak passwords."
                else -> "High risk. Immediate attention recommended."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }

          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(
                when {
                  auditStats.securityScore >= 85 -> SecurityGreen.copy(alpha = 0.15f)
                  auditStats.securityScore >= 60 -> SecurityAmber.copy(alpha = 0.15f)
                  else -> SecurityRed.copy(alpha = 0.15f)
                }
              ),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${auditStats.securityScore}",
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = when {
                auditStats.securityScore >= 85 -> SecurityGreen
                auditStats.securityScore >= 60 -> SecurityAmber
                else -> SecurityRed
              }
            )
          }
        }
      }
    }

    // Runtime Isolation Signal & Access Detection Guard
    item {
      RuntimeSecuritySignalCard(
        report = securityReport,
        accessLogs = accessLogs,
        onToggleExternalAccess = onToggleExternalAccess,
        onRunScan = onRunRuntimeScan,
        onSimulateAccessAttempt = onSimulateAccessAttempt,
        onSimulateThreatToggle = onSimulateThreatToggle,
        onClearLogs = onClearAccessLogs
      )
    }

    // Stats Grid
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        AuditMetricCard(
          title = "Total Vault",
          value = "${auditStats.totalItems}",
          icon = Icons.Default.Key,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f)
        )
        AuditMetricCard(
          title = "Strong",
          value = "${auditStats.strongPasswords}",
          icon = Icons.Default.CheckCircle,
          color = SecurityGreen,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        AuditMetricCard(
          title = "Weak",
          value = "${auditStats.weakPasswords}",
          icon = Icons.Default.Warning,
          color = if (auditStats.weakPasswords > 0) SecurityRed else SecurityGreen,
          modifier = Modifier.weight(1f)
        )
        AuditMetricCard(
          title = "Reused",
          value = "${auditStats.reusedPasswords}",
          icon = Icons.Default.Repeat,
          color = if (auditStats.reusedPasswords > 0) SecurityAmber else SecurityGreen,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Weak Passwords Section
    if (weakItems.isNotEmpty()) {
      item {
        Text(
          text = "Weak Passwords (${weakItems.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = SecurityRed,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
      items(weakItems) { item ->
        AuditItemRow(
          item = item,
          issue = "Low entropy password",
          issueColor = SecurityRed,
          onEdit = { onEditItem(item) }
        )
      }
    }

    // Reused Passwords Section
    if (reusedItems.isNotEmpty()) {
      item {
        Text(
          text = "Reused Passwords (${reusedItems.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = SecurityAmber,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
      items(reusedItems) { item ->
        AuditItemRow(
          item = item,
          issue = "Password shared with other accounts",
          issueColor = SecurityAmber,
          onEdit = { onEditItem(item) }
        )
      }
    }

    if (weakItems.isEmpty() && reusedItems.isEmpty() && vaultItems.isNotEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = SecurityGreen.copy(alpha = 0.1f)
          )
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = SecurityGreen,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "All Stored Passwords Look Solid",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SecurityGreen
              )
              Text(
                text = "No weak or reused passwords detected across your encrypted vault items.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
              )
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun AuditMetricCard(
  title: String,
  value: String,
  icon: ImageVector,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = color
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = color
      )
    }
  }
}

@Composable
private fun AuditItemRow(
  item: VaultItem,
  issue: String,
  issueColor: Color,
  onEdit: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        if (item.username.isNotEmpty()) {
          Text(
            text = item.username,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )
        }
        Text(
          text = issue,
          style = MaterialTheme.typography.labelSmall,
          color = issueColor,
          fontWeight = FontWeight.Medium
        )
      }
      IconButton(onClick = onEdit) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Edit",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}
