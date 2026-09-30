package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.RuntimeSecurityReport
import com.example.security.SignalLevel
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityRed

@Composable
fun RuntimeSignalPill(
  report: RuntimeSecurityReport,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val signalColor = when (report.signal) {
    SignalLevel.GREEN_SECURE -> SecurityGreen
    SignalLevel.YELLOW_WARNING -> SecurityAmber
    SignalLevel.RED_ALERT -> SecurityRed
  }

  val animatedColor by animateColorAsState(
    targetValue = signalColor,
    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
    label = "pill_color_anim"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (report.signal != SignalLevel.GREEN_SECURE) 1.35f else 1.1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pill_pulse"
  )

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = animatedColor.copy(alpha = 0.15f),
    border = androidx.compose.foundation.BorderStroke(1.dp, animatedColor.copy(alpha = 0.35f)),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag("runtime_signal_pill")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(animatedColor.copy(alpha = 0.3f))
        )
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(animatedColor)
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Text(
        text = when (report.signal) {
          SignalLevel.GREEN_SECURE -> "Isolated (Green)"
          SignalLevel.YELLOW_WARNING -> "Open Access (Yellow)"
          SignalLevel.RED_ALERT -> "Threat Blocked (Red)"
        },
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = animatedColor
      )
    }
  }
}
