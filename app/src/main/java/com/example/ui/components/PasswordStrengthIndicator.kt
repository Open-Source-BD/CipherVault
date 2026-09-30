package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager
import com.example.crypto.PasswordStrengthInfo
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityLime
import com.example.ui.theme.SecurityOrange
import com.example.ui.theme.SecurityRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasswordStrengthIndicator(
  password: String,
  modifier: Modifier = Modifier,
  showDetailedBadges: Boolean = true,
  strengthInfo: PasswordStrengthInfo = remember(password) { CryptoManager.calculatePasswordStrength(password) }
) {
  val score = strengthInfo.score // 1 to 5

  // Primary active color based on score (Red -> Orange -> Yellow/Amber -> Lime -> Emerald Green)
  val targetColor = when (score) {
    1 -> SecurityRed
    2 -> SecurityOrange
    3 -> SecurityAmber
    4 -> SecurityLime
    5 -> SecurityGreen
    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
  }

  val animatedColor by animateColorAsState(
    targetValue = targetColor,
    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
    label = "strength_color"
  )

  val animatedProgress by animateFloatAsState(
    targetValue = (score / 5f).coerceIn(0.1f, 1f),
    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
    label = "strength_progress"
  )

  // Character breakdown metrics
  val hasUpper = remember(password) { password.any { it.isUpperCase() } }
  val hasLower = remember(password) { password.any { it.isLowerCase() } }
  val hasDigit = remember(password) { password.any { it.isDigit() } }
  val hasSymbol = remember(password) { password.any { !it.isLetterOrDigit() } }
  val length = password.length

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("password_strength_indicator_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Top Row: Strength Label + Live Entropy Counter
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(animatedColor)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strengthInfo.label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = animatedColor
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = animatedColor.copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, animatedColor.copy(alpha = 0.3f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = null,
              tint = animatedColor,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${strengthInfo.entropyBits.toInt()} bits entropy",
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              ),
              color = animatedColor
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 5-Segment Visual Progress Bar (Color-coded Red, Orange, Yellow, Lime, Green)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val segmentColors = listOf(
          SecurityRed,
          SecurityOrange,
          SecurityAmber,
          SecurityLime,
          SecurityGreen
        )

        for (i in 0 until 5) {
          val isSegmentActive = i < score
          val segmentColor = if (isSegmentActive) segmentColors[i] else MaterialTheme.colorScheme.surfaceVariant

          Box(
            modifier = Modifier
              .weight(1f)
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(segmentColor)
              .testTag("strength_segment_$i")
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Estimated Crack Time & Brute-force resistance readout
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "Crack Time: ",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
          Text(
            text = strengthInfo.estimatedCrackTime,
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            ),
            color = animatedColor
          )
        }
      }

      // Detailed Composition Criteria Badges (Length, Uppercase, Lowercase, Numbers, Symbols)
      if (showDetailedBadges) {
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          CriteriaChip(
            label = "$length chars",
            isSatisfied = length >= 12,
            isWarning = length in 8..11
          )
          CriteriaChip(
            label = "A-Z",
            isSatisfied = hasUpper,
            isWarning = false
          )
          CriteriaChip(
            label = "a-z",
            isSatisfied = hasLower,
            isWarning = false
          )
          CriteriaChip(
            label = "0-9",
            isSatisfied = hasDigit,
            isWarning = false
          )
          CriteriaChip(
            label = "!@#$",
            isSatisfied = hasSymbol,
            isWarning = false
          )
        }
      }

      // Guidance Note
      if (strengthInfo.advice.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = strengthInfo.advice,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
      }
    }
  }
}

@Composable
private fun CriteriaChip(
  label: String,
  isSatisfied: Boolean,
  isWarning: Boolean
) {
  val (chipColor, textColor, icon) = when {
    isSatisfied -> Triple(
      SecurityGreen.copy(alpha = 0.15f),
      SecurityGreen,
      Icons.Default.Check
    )
    isWarning -> Triple(
      SecurityAmber.copy(alpha = 0.15f),
      SecurityAmber,
      Icons.Default.Check
    )
    else -> Triple(
      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
      Icons.Default.Close
    )
  }

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = chipColor,
    border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.25f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(11.dp),
        tint = textColor
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = textColor
      )
    }
  }
}
