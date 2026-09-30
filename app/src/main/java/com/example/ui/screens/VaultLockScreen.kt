package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager
import com.example.ui.LockState
import com.example.ui.components.PasswordStrengthIndicator
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityRed

@Composable
fun VaultLockScreen(
  lockState: LockState,
  isBiometricAvailable: Boolean,
  isBiometricEnabled: Boolean,
  onUnlockWithPassword: (String) -> Boolean,
  onUnlockWithBiometric: () -> Unit,
  onSetupMasterPassword: (String, Boolean) -> Boolean,
  modifier: Modifier = Modifier
) {
  val isSetup = lockState == LockState.NOT_INITIALIZED

  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var enableBiometricOnSetup by remember { mutableStateOf(isBiometricAvailable) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val strength = remember(password) { CryptoManager.calculatePasswordStrength(password) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      // Shield Lock Icon with Glowing Ring
      Box(
        modifier = Modifier
          .size(88.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isSetup) Icons.Default.Shield else Icons.Default.Lock,
          contentDescription = "Security Shield",
          modifier = Modifier.size(46.dp),
          tint = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = if (isSetup) "Create Master Vault" else "CipherVault Locked",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onBackground
      )

      Text(
        text = if (isSetup)
          "Set a strong Master Password. It derives your local AES-256-GCM encryption key. If lost, your data cannot be recovered."
        else
          "Enter your master password or authenticate with biometrics to decrypt your local vault.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Master Password Input
      OutlinedTextField(
        value = password,
        onValueChange = {
          password = it
          errorMessage = null
        },
        label = { Text(if (isSetup) "Master Password (min 6 chars)" else "Master Password") },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Password,
          imeAction = if (isSetup) ImeAction.Next else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            if (!isSetup) {
              val success = onUnlockWithPassword(password)
              if (!success) errorMessage = "Invalid master password. Decryption failed."
            }
          }
        ),
        trailingIcon = {
          IconButton(
            onClick = { passwordVisible = !passwordVisible },
            modifier = Modifier.testTag("toggle_password_visibility")
          ) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (passwordVisible) "Hide password" else "Show password"
            )
          }
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("master_password_input")
      )

      if (isSetup && password.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        PasswordStrengthIndicator(
          password = password,
          strengthInfo = strength,
          showDetailedBadges = true,
          modifier = Modifier.fillMaxWidth()
        )
      }

      if (isSetup) {
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = confirmPassword,
          onValueChange = {
            confirmPassword = it
            errorMessage = null
          },
          label = { Text("Confirm Master Password") },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (password != confirmPassword) {
                errorMessage = "Passwords do not match."
              } else if (password.length < 6) {
                errorMessage = "Password must be at least 6 characters."
              } else {
                val success = onSetupMasterPassword(password, enableBiometricOnSetup)
                if (!success) errorMessage = "Failed to initialize vault."
              }
            }
          ),
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.LockOpen,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_password_input")
        )

        if (isBiometricAvailable) {
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = enableBiometricOnSetup,
              onCheckedChange = { enableBiometricOnSetup = it },
              colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.testTag("setup_biometric_checkbox")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Enable Biometric / Device Unlock",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "Quick unlock with fingerprint, face scan, or PIN",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
              )
            }
          }
        }
      }

      AnimatedVisibility(visible = errorMessage != null) {
        errorMessage?.let {
          Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Primary Action Button
      Button(
        onClick = {
          if (isSetup) {
            if (password.length < 6) {
              errorMessage = "Password must be at least 6 characters."
            } else if (password != confirmPassword) {
              errorMessage = "Passwords do not match."
            } else {
              val success = onSetupMasterPassword(password, enableBiometricOnSetup)
              if (!success) errorMessage = "Failed to initialize vault."
            }
          } else {
            val success = onUnlockWithPassword(password)
            if (!success) errorMessage = "Invalid master password. Decryption failed."
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("unlock_action_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Icon(
          imageVector = if (isSetup) Icons.Default.Shield else Icons.Default.LockOpen,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isSetup) "Create Encrypted Vault" else "Unlock Vault",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }

      // Biometric Quick Unlock Button (if in unlock mode and biometric enabled)
      if (!isSetup && isBiometricAvailable && isBiometricEnabled) {
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedButton(
          onClick = onUnlockWithBiometric,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("biometric_unlock_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Biometric Icon",
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Unlock with Biometrics / PIN",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Minimalist Security Transparency Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = CyanAccent
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Open-Source Security Guarantees",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "• 100% Offline • Zero Network / INTERNET permissions\n" +
                "• Local AES-256-GCM encryption with 100,000 PBKDF2 rounds\n" +
                "• Anti-screenshot FLAG_SECURE protects against rogue apps\n" +
                "• Encrypted local backups can be exported anytime",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
