package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.crypto.CryptoManager
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.ui.components.PasswordStrengthIndicator
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditItemDialog(
  initialItem: VaultItem? = null,
  onDismiss: () -> Unit,
  onSave: (VaultItem) -> Unit
) {
  var title by remember { mutableStateOf(initialItem?.title ?: "") }
  var username by remember { mutableStateOf(initialItem?.username ?: "") }
  var password by remember { mutableStateOf(initialItem?.password ?: "") }
  var website by remember { mutableStateOf(initialItem?.website ?: "") }
  var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
  var category by remember { mutableStateOf(initialItem?.category ?: VaultCategory.LOGINS) }
  var isFavorite by remember { mutableStateOf(initialItem?.isFavorite ?: false) }
  var passwordVisible by remember { mutableStateOf(false) }

  val strength = remember(password) { CryptoManager.calculatePasswordStrength(password) }

  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val titleRequester = remember { BringIntoViewRequester() }
  val usernameRequester = remember { BringIntoViewRequester() }
  val passwordRequester = remember { BringIntoViewRequester() }
  val websiteRequester = remember { BringIntoViewRequester() }
  val notesRequester = remember { BringIntoViewRequester() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .imePadding()
        .clip(RoundedCornerShape(20.dp)),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (initialItem == null) "Add Vault Item" else "Edit Item",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Row {
            IconButton(
              onClick = { isFavorite = !isFavorite },
              modifier = Modifier.testTag("toggle_item_favorite_btn")
            ) {
              Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = "Favorite",
                tint = if (isFavorite) SecurityAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("close_add_edit_dialog_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Selector Chips
        Text(
          text = "Category",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            VaultCategory.LOGINS,
            VaultCategory.CARDS,
            VaultCategory.SECURE_NOTES,
            VaultCategory.IDENTITIES
          ).forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title Input
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title / Service Name (Required)") },
          placeholder = { Text("e.g. Google, Proton, Work VPN") },
          leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(titleRequester)
            .onFocusChanged {
              if (it.isFocused) {
                coroutineScope.launch {
                  delay(100)
                  titleRequester.bringIntoView()
                }
              }
            }
            .testTag("item_title_input"),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
          ),
          keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Username / Email Input
        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = { Text("Username / Email / Account") },
          placeholder = { Text("name@example.com or user123") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(usernameRequester)
            .onFocusChanged {
              if (it.isFocused) {
                coroutineScope.launch {
                  delay(100)
                  usernameRequester.bringIntoView()
                }
              }
            }
            .testTag("item_username_input"),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
          ),
          keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Input with inline Generator Button
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password / Secret Key") },
          leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
          trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  password = CryptoManager.generatePassword(18)
                  passwordVisible = true
                },
                modifier = Modifier.testTag("inline_generate_password_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = "Generate Strong Password",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
              IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.testTag("inline_toggle_password_visibility")
              ) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
              }
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(passwordRequester)
            .onFocusChanged {
              if (it.isFocused) {
                coroutineScope.launch {
                  delay(100)
                  passwordRequester.bringIntoView()
                }
              }
            }
            .testTag("item_password_input"),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
          ),
          keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
          )
        )

        // Password Strength indicator
        if (password.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          PasswordStrengthIndicator(
            password = password,
            strengthInfo = strength,
            showDetailedBadges = false,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Website URL Input
        OutlinedTextField(
          value = website,
          onValueChange = { website = it },
          label = { Text("Website URL (Optional)") },
          placeholder = { Text("https://example.com") },
          leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(websiteRequester)
            .onFocusChanged {
              if (it.isFocused) {
                coroutineScope.launch {
                  delay(100)
                  websiteRequester.bringIntoView()
                }
              }
            }
            .testTag("item_website_input"),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next
          ),
          keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Notes (Encrypted)
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Secure Notes (Encrypted)") },
          placeholder = { Text("Recovery codes, PINs, security questions...") },
          leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(notesRequester)
            .onFocusChanged {
              if (it.isFocused) {
                coroutineScope.launch {
                  delay(100)
                  notesRequester.bringIntoView()
                }
              }
            }
            .testTag("item_notes_input"),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              keyboardController?.hide()
              focusManager.clearFocus()
            }
          )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(12.dp))
          Button(
            onClick = {
              if (title.isNotBlank()) {
                val item = (initialItem ?: VaultItem(title = title)).copy(
                  title = title.trim(),
                  username = username.trim(),
                  password = password,
                  website = website.trim(),
                  notes = notes.trim(),
                  category = category,
                  isFavorite = isFavorite
                )
                onSave(item)
                onDismiss()
              }
            },
            enabled = title.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("save_vault_item_btn")
          ) {
            Text(if (initialItem == null) "Save Encrypted" else "Save Changes")
          }
        }
      }
    }
  }
}
