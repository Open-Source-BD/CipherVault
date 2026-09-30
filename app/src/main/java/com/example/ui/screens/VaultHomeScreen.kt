package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.security.AccessEvent
import com.example.security.RuntimeSecurityReport
import com.example.ui.components.RuntimeSecuritySignalCard
import com.example.ui.components.RuntimeSignalPill
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SecurityAmber

@Composable
fun VaultHomeScreen(
  items: List<VaultItem>,
  searchQuery: String,
  selectedCategory: VaultCategory,
  showFavoritesOnly: Boolean,
  revealedItemIds: Set<Long>,
  clipboardCountdown: Int?,
  securityReport: RuntimeSecurityReport,
  accessLogs: List<AccessEvent>,
  onToggleExternalAccess: (Boolean) -> Unit,
  onRunRuntimeScan: () -> Unit,
  onSimulateAccessAttempt: () -> Unit,
  onSimulateThreatToggle: (Boolean) -> Unit,
  onClearAccessLogs: () -> Unit,
  onSearchChange: (String) -> Unit,
  onCategorySelect: (VaultCategory) -> Unit,
  onToggleFavoritesOnly: () -> Unit,
  onRequestPasswordReveal: (VaultItem) -> Unit,
  onHidePassword: (Long) -> Unit,
  onRequestPasswordCopy: (VaultItem) -> Unit,
  onCopyUsername: (String) -> Unit,
  onToggleFavorite: (VaultItem) -> Unit,
  onEditItem: (VaultItem) -> Unit,
  onDeleteItem: (VaultItem) -> Unit,
  onAddNewItem: () -> Unit,
  onLockVault: () -> Unit,
  onClearClipboard: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isSearchExpanded by remember { mutableStateOf(false) }
  var showSecurityGuardDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onAddNewItem,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = Modifier.testTag("add_item_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Item")
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "CipherVault",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(EmeraldAccent)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "100% Offline • Hardware Secured",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          RuntimeSignalPill(
            report = securityReport,
            onClick = { showSecurityGuardDialog = true }
          )
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = { isSearchExpanded = !isSearchExpanded },
            modifier = Modifier.testTag("toggle_search_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.onBackground
            )
          }
          IconButton(
            onClick = onLockVault,
            modifier = Modifier.testTag("quick_lock_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Lock Vault",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
          }
        }
      }

      // Search Field (if expanded or text present)
      AnimatedVisibility(visible = isSearchExpanded || searchQuery.isNotEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by title, username, url...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_vault_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
          )
        }
      }

      // Category filter chips
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          FilterChip(
            selected = showFavoritesOnly,
            onClick = onToggleFavoritesOnly,
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (showFavoritesOnly) SecurityAmber else MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            label = { Text("Favorites", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("filter_favorites")
          )
        }

        items(VaultCategory.values()) { category ->
          FilterChip(
            selected = !showFavoritesOnly && selectedCategory == category,
            onClick = {
              if (showFavoritesOnly) onToggleFavoritesOnly()
              onCategorySelect(category)
            },
            label = { Text(category.displayName, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("filter_cat_${category.name}")
          )
        }
      }

      // Clipboard auto-clear alert banner
      AnimatedVisibility(visible = clipboardCountdown != null) {
        clipboardCountdown?.let { remaining ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Clipboard auto-clearing in ${remaining}s",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
              TextButton(
                onClick = onClearClipboard,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
              ) {
                Text("Clear Now", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }

      // Items List
      if (items.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = if (searchQuery.isNotEmpty()) "No matching credentials" else "Your Vault is Empty",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (searchQuery.isNotEmpty())
                "Try searching for another service or clear the search query."
              else
                "Add your logins, payment cards, or secure notes. All data is encrypted locally with AES-256-GCM.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (searchQuery.isEmpty()) {
              Spacer(modifier = Modifier.height(16.dp))
              Button(
                onClick = onAddNewItem,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                )
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Item")
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(items, key = { it.id }) { item ->
            val isRevealed = revealedItemIds.contains(item.id)
            VaultItemCard(
              item = item,
              isRevealed = isRevealed,
              onRequestReveal = { onRequestPasswordReveal(item) },
              onHide = { onHidePassword(item.id) },
              onRequestCopy = { onRequestPasswordCopy(item) },
              onCopyUsername = { onCopyUsername(item.username) },
              onToggleFavorite = { onToggleFavorite(item) },
              onEdit = { onEditItem(item) },
              onDelete = { onDeleteItem(item) }
            )
          }

          item {
            Spacer(modifier = Modifier.height(72.dp))
          }
        }
      }
    }
  }

  if (showSecurityGuardDialog) {
    AlertDialog(
      onDismissRequest = { showSecurityGuardDialog = false },
      confirmButton = {
        TextButton(onClick = { showSecurityGuardDialog = false }) {
          Text("Close")
        }
      },
      title = null,
      text = {
        Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
      }
    )
  }
}

@Composable
fun VaultItemCard(
  item: VaultItem,
  isRevealed: Boolean,
  onRequestReveal: () -> Unit,
  onHide: () -> Unit,
  onRequestCopy: () -> Unit,
  onCopyUsername: () -> Unit,
  onToggleFavorite: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var menuExpanded by remember { mutableStateOf(false) }

  val categoryIcon = when (item.category) {
    VaultCategory.LOGINS -> Icons.Default.Key
    VaultCategory.CARDS -> Icons.Default.CreditCard
    VaultCategory.SECURE_NOTES -> Icons.Default.Notes
    VaultCategory.IDENTITIES -> Icons.Default.Person
    else -> Icons.Default.Shield
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("vault_card_${item.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top row: Category icon, Title, Favorite & Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = categoryIcon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (item.username.isNotEmpty()) {
              Text(
                text = item.username,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = "Favorite",
              tint = if (item.isFavorite) SecurityAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
              modifier = Modifier.size(20.dp)
            )
          }

          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
              )
            }

            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false }
            ) {
              if (item.username.isNotEmpty()) {
                DropdownMenuItem(
                  text = { Text("Copy Username") },
                  onClick = {
                    menuExpanded = false
                    onCopyUsername()
                  },
                  leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
              }
              DropdownMenuItem(
                text = { Text("Edit Item") },
                onClick = {
                  menuExpanded = false
                  onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
              )
              DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                  menuExpanded = false
                  onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
              )
            }
          }
        }
      }

      // Password row if password exists
      if (item.password.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isRevealed) item.password else "••••••••••••••••",
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isRevealed) FontWeight.SemiBold else FontWeight.Bold,
                letterSpacing = if (isRevealed) 0.5.sp else 2.sp
              ),
              color = if (isRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            // Eye Toggle (Triggers biometric verification when requested to reveal)
            IconButton(
              onClick = {
                if (isRevealed) onHide() else onRequestReveal()
              },
              modifier = Modifier
                .size(34.dp)
                .testTag("reveal_btn_${item.id}")
            ) {
              Icon(
                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (isRevealed) "Hide Password" else "Reveal Password with Biometric",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }

            // Copy Button (Triggers biometric check if configured, then copies password)
            IconButton(
              onClick = onRequestCopy,
              modifier = Modifier
                .size(34.dp)
                .testTag("copy_password_btn_${item.id}")
            ) {
              Icon(
                imageVector = Icons.Default.Key,
                contentDescription = "Copy Password",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // Notes snippet if present
      if (item.notes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = item.notes,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
