package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.ui.components.PasswordStrengthIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PasswordGeneratorScreen(
  onCopyPassword: (String) -> Unit,
  onSaveToVault: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Characters, 1 = Passphrase

  // Character Mode Options
  var length by remember { mutableFloatStateOf(18f) }
  var includeUpper by remember { mutableStateOf(true) }
  var includeLower by remember { mutableStateOf(true) }
  var includeDigits by remember { mutableStateOf(true) }
  var includeSymbols by remember { mutableStateOf(true) }
  var avoidAmbiguous by remember { mutableStateOf(true) }

  // Advanced Character Settings
  var showAdvancedOptions by remember { mutableStateOf(false) }
  var customSymbols by remember { mutableStateOf("!@#\$%^&*()-_=+[]{}|;:,.<>?") }
  var excludedCharacters by remember { mutableStateOf("") }

  // Passphrase Mode Options
  var wordsCount by remember { mutableFloatStateOf(4f) }
  var separator by remember { mutableStateOf("-") }

  // Session History (in-memory only for security)
  val sessionHistory = remember { mutableStateListOf<String>() }
  var showHistory by remember { mutableStateOf(false) }

  // Generated Text
  var generatedText by remember {
    mutableStateOf(
      CryptoManager.generatePassword(
        length = 18,
        includeUppercase = true,
        includeLowercase = true,
        includeDigits = true,
        includeSymbols = true,
        avoidAmbiguous = true
      )
    )
  }

  var copiedEffect by remember { mutableStateOf(false) }

  fun regenerate() {
    val newPassword = if (selectedTab == 0) {
      CryptoManager.generatePassword(
        length = length.toInt(),
        includeUppercase = includeUpper,
        includeLowercase = includeLower,
        includeDigits = includeDigits,
        includeSymbols = includeSymbols,
        avoidAmbiguous = avoidAmbiguous,
        customSymbols = customSymbols,
        excludedCharacters = excludedCharacters
      )
    } else {
      CryptoManager.generatePassphrase(
        wordsCount = wordsCount.toInt(),
        separator = separator
      )
    }

    if (generatedText.isNotEmpty() && generatedText != newPassword) {
      if (!sessionHistory.contains(generatedText)) {
        sessionHistory.add(0, generatedText)
        if (sessionHistory.size > 8) sessionHistory.removeLast()
      }
    }
    generatedText = newPassword
  }

  // Preset appliers
  fun applyPreset(presetLength: Int, upper: Boolean, lower: Boolean, digits: Boolean, symbols: Boolean, ambiguous: Boolean) {
    selectedTab = 0
    length = presetLength.toFloat()
    includeUpper = upper
    includeLower = lower
    includeDigits = digits
    includeSymbols = symbols
    avoidAmbiguous = ambiguous
    regenerate()
  }

  val strength = remember(generatedText) { CryptoManager.calculatePasswordStrength(generatedText) }

  // Character syntax highlighter (colors numbers cyan, symbols amber, letters primary)
  val formattedPassword = remember(generatedText) {
    buildAnnotatedString {
      for (char in generatedText) {
        when {
          char.isDigit() -> withStyle(SpanStyle(color = CyanAccent, fontWeight = FontWeight.Bold)) {
            append(char)
          }
          !char.isLetterOrDigit() -> withStyle(SpanStyle(color = SecurityAmber, fontWeight = FontWeight.Bold)) {
            append(char)
          }
          char.isUpperCase() -> withStyle(SpanStyle(color = EmeraldAccent, fontWeight = FontWeight.SemiBold)) {
            append(char)
          }
          else -> withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
            append(char)
          }
        }
      }
    }
  }

  // Count character breakdown
  val upperCount = remember(generatedText) { generatedText.count { it.isUpperCase() } }
  val lowerCount = remember(generatedText) { generatedText.count { it.isLowerCase() } }
  val digitCount = remember(generatedText) { generatedText.count { it.isDigit() } }
  val symbolCount = remember(generatedText) { generatedText.count { !it.isLetterOrDigit() } }

  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val isImeVisible = WindowInsets.isImeVisible
  val symbolsRequester = remember { BringIntoViewRequester() }
  val excludedRequester = remember { BringIntoViewRequester() }
  val passwordInputRequester = remember { BringIntoViewRequester() }
  var isDirectEditMode by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .imePadding()
      .padding(horizontal = 20.dp)
      .verticalScroll(scrollState),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    // Floating Keyboard Active Notification Bar
    AnimatedVisibility(
      visible = isImeVisible,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Editing input field",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          TextButton(
            onClick = {
              keyboardController?.hide()
              focusManager.clearFocus()
            }
          ) {
            Icon(
              imageVector = Icons.Default.KeyboardHide,
              contentDescription = "Close keyboard",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Close Keyboard",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }

    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Entropy Generator",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "Hardware-grade cryptographically sound random generation",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
      }
      IconButton(
        onClick = { showHistory = !showHistory },
        modifier = Modifier.testTag("toggle_generator_history_btn")
      ) {
        Icon(
          imageVector = Icons.Default.History,
          contentDescription = "Generation History",
          tint = if (showHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Presets Row
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      item {
        FilterChip(
          selected = selectedTab == 0 && length.toInt() == 16 && includeSymbols && avoidAmbiguous,
          onClick = { applyPreset(16, upper = true, lower = true, digits = true, symbols = true, ambiguous = true) },
          label = { Text("Balanced (16)") },
          leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp)) }
        )
      }
      item {
        FilterChip(
          selected = selectedTab == 0 && length.toInt() == 32 && includeSymbols,
          onClick = { applyPreset(32, upper = true, lower = true, digits = true, symbols = true, ambiguous = false) },
          label = { Text("Ultra (32)") },
          leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp)) }
        )
      }
      item {
        FilterChip(
          selected = selectedTab == 0 && length.toInt() == 16 && !includeSymbols,
          onClick = { applyPreset(16, upper = true, lower = true, digits = true, symbols = false, ambiguous = true) },
          label = { Text("Letters & Digits") }
        )
      }
      item {
        FilterChip(
          selected = selectedTab == 0 && length.toInt() == 6 && !includeUpper && !includeLower && includeDigits && !includeSymbols,
          onClick = { applyPreset(6, upper = false, lower = false, digits = true, symbols = false, ambiguous = false) },
          label = { Text("PIN (6)") },
          leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(14.dp)) }
        )
      }
      item {
        FilterChip(
          selected = selectedTab == 1,
          onClick = {
            selectedTab = 1
            regenerate()
          },
          label = { Text("Passphrase") }
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // In-Session History Dialog/Sheet
    AnimatedVisibility(visible = showHistory && sessionHistory.isNotEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 500.dp)
          .padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Session History (RAM Only)",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { sessionHistory.clear() }) {
              Text("Clear", style = MaterialTheme.typography.labelSmall)
            }
          }
          sessionHistory.forEach { histItem ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                .clickable {
                  generatedText = histItem
                  onCopyPassword(histItem)
                  copiedEffect = true
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = histItem,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
              Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }

    // Generated Password Display Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      )
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Toggle Row between Formatted Monospace View and Editable Input Field
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isDirectEditMode) "Custom Input / Test Mode" else "Generated Result",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          TextButton(
            onClick = {
              isDirectEditMode = !isDirectEditMode
              if (isDirectEditMode) {
                coroutineScope.launch {
                  delay(100)
                  passwordInputRequester.bringIntoView()
                }
              }
            },
            modifier = Modifier.testTag("toggle_direct_edit_btn")
          ) {
            Icon(
              imageVector = if (isDirectEditMode) Icons.Default.Visibility else Icons.Default.Edit,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isDirectEditMode) "Show Highlighted" else "Type / Edit Input",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        if (isDirectEditMode) {
          OutlinedTextField(
            value = generatedText,
            onValueChange = {
              generatedText = it
            },
            label = { Text("Password / Passphrase Input") },
            placeholder = { Text("Type password to analyze entropy...") },
            textStyle = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 1.sp
            ),
            singleLine = true,
            trailingIcon = {
              if (generatedText.isNotEmpty()) {
                IconButton(onClick = { generatedText = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear input")
                }
              }
            },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Ascii,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
              }
            ),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
              .fillMaxWidth()
              .bringIntoViewRequester(passwordInputRequester)
              .onFocusChanged {
                if (it.isFocused) {
                  coroutineScope.launch {
                    delay(100)
                    passwordInputRequester.bringIntoView()
                  }
                }
              }
              .testTag("generated_password_input_box")
          )
        } else {
          // Monospace text box with syntax highlighting (clickable to edit)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.background)
              .clickable {
                isDirectEditMode = true
                coroutineScope.launch {
                  delay(100)
                  passwordInputRequester.bringIntoView()
                }
              }
              .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = formattedPassword,
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                lineHeight = 26.sp
              ),
              color = MaterialTheme.colorScheme.onBackground,
              textAlign = TextAlign.Center,
              modifier = Modifier.testTag("generated_password_display")
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Password Strength & Entropy Indicator (Real-Time)
        PasswordStrengthIndicator(
          password = generatedText,
          strengthInfo = strength,
          showDetailedBadges = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Copy, Generate, Save to Vault
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              onCopyPassword(generatedText)
              copiedEffect = true
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("copy_generated_password_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Icon(
              imageVector = if (copiedEffect) Icons.Default.Check else Icons.Default.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (copiedEffect) "Copied!" else "Copy",
              fontWeight = FontWeight.Bold
            )
          }

          Button(
            onClick = {
              copiedEffect = false
              regenerate()
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("regenerate_password_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Regenerate", fontWeight = FontWeight.Bold)
          }
        }

        if (onSaveToVault != null) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = { onSaveToVault(generatedText) },
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp)
              .testTag("save_generated_to_vault_btn"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Directly to Vault", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Generator Mode Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
      contentColor = MaterialTheme.colorScheme.primary,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp)
        .clip(RoundedCornerShape(14.dp))
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = {
          selectedTab = 0
          regenerate()
        },
        text = { Text("Character Set", fontWeight = FontWeight.SemiBold) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = {
          selectedTab = 1
          regenerate()
        },
        text = { Text("Memorable Passphrase", fontWeight = FontWeight.SemiBold) }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Customization Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      )
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        if (selectedTab == 0) {
          // Length Control with Stepper & Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Password Length",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  if (length > 4f) {
                    length -= 1f
                    regenerate()
                  }
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease length", modifier = Modifier.size(16.dp))
              }
              Text(
                text = "${length.toInt()}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
              )
              IconButton(
                onClick = {
                  if (length < 64f) {
                    length += 1f
                    regenerate()
                  }
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = "Increase length", modifier = Modifier.size(16.dp))
              }
            }
          }
          Slider(
            value = length,
            onValueChange = {
              length = it
              regenerate()
            },
            valueRange = 4f..64f,
            steps = 59,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("length_slider")
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

          // Character Set Toggles
          ToggleRow("Uppercase Letters", "A, B, C... Z ($upperCount in result)", includeUpper) {
            includeUpper = it
            regenerate()
          }
          ToggleRow("Lowercase Letters", "a, b, c... z ($lowerCount in result)", includeLower) {
            includeLower = it
            regenerate()
          }
          ToggleRow("Numbers / Digits", "0, 1, 2... 9 ($digitCount in result)", includeDigits) {
            includeDigits = it
            regenerate()
          }
          ToggleRow("Special Symbols", "!@#$%^&* ($symbolCount in result)", includeSymbols) {
            includeSymbols = it
            regenerate()
          }
          ToggleRow("Avoid Lookalike Characters", "Excludes ambiguous: 0/O, 1/l/I", avoidAmbiguous) {
            avoidAmbiguous = it
            regenerate()
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

          // Advanced Options Accordion
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                showAdvancedOptions = !showAdvancedOptions
                if (showAdvancedOptions) {
                  coroutineScope.launch {
                    delay(150)
                    symbolsRequester.bringIntoView()
                  }
                }
              }
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Advanced Rule Customization",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Icon(
              imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          }

          AnimatedVisibility(visible = showAdvancedOptions) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
              OutlinedTextField(
                value = customSymbols,
                onValueChange = {
                  customSymbols = it
                  regenerate()
                },
                label = { Text("Allowed Symbols Pool") },
                singleLine = true,
                trailingIcon = {
                  if (customSymbols.isNotEmpty()) {
                    IconButton(onClick = { customSymbols = ""; regenerate() }) {
                      Icon(Icons.Default.Clear, contentDescription = "Clear symbols")
                    }
                  }
                },
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Ascii,
                  imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                  onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .bringIntoViewRequester(symbolsRequester)
                  .onFocusChanged {
                    if (it.isFocused) {
                      coroutineScope.launch {
                        delay(100)
                        symbolsRequester.bringIntoView()
                      }
                    }
                  }
                  .testTag("custom_symbols_input")
              )
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedTextField(
                value = excludedCharacters,
                onValueChange = {
                  excludedCharacters = it
                  regenerate()
                },
                label = { Text("Exclude Specific Characters") },
                placeholder = { Text("e.g. \" ' ` ~") },
                singleLine = true,
                trailingIcon = {
                  if (excludedCharacters.isNotEmpty()) {
                    IconButton(onClick = { excludedCharacters = ""; regenerate() }) {
                      Icon(Icons.Default.Clear, contentDescription = "Clear excluded chars")
                    }
                  }
                },
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Ascii,
                  imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                  onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                  }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .bringIntoViewRequester(excludedRequester)
                  .onFocusChanged {
                    if (it.isFocused) {
                      coroutineScope.launch {
                        delay(100)
                        excludedRequester.bringIntoView()
                      }
                    }
                  }
                  .testTag("excluded_chars_input")
              )
              Spacer(modifier = Modifier.height(16.dp))
            }
          }
        } else {
          // Passphrase Customization Mode
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Number of Words",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${wordsCount.toInt()} words",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }
          Slider(
            value = wordsCount,
            onValueChange = {
              wordsCount = it
              regenerate()
            },
            valueRange = 3f..8f,
            steps = 4,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary
            )
          )

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Word Separator",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              Pair("-", "Hyphen (-)"),
              Pair(".", "Dot (.)"),
              Pair("_", "Underscore (_)"),
              Pair(" ", "Space ( )")
            ).forEach { (sep, label) ->
              FilterChip(
                selected = separator == sep,
                onClick = {
                  separator = sep
                  regenerate()
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}

@Composable
private fun ToggleRow(
  label: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 5.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(
        text = label,
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
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
      )
    )
  }
}
